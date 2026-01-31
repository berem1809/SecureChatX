import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import api from '../../services/api';
import { Conversation, Message, User } from '../../types';
import EncryptionService, { EncryptedMessage, SharedSecret } from '../../services/encryption';
import KeyExchangeService from '../../services/keyExchange';

interface ChatState {
  conversations: Conversation[];
  selectedConversation: Conversation | null;
  messages: Message[];
  isLoading: boolean;
  error: string | null;
  chatType: 'direct' | 'group'; // Track which tab is active
}

const initialState: ChatState = {
  conversations: [],
  selectedConversation: null,
  messages: [],
  isLoading: false,
  error: null,
  chatType: 'direct', // Default to direct chat
};

// Helper to map backend conversation response to frontend type
const mapConversation = (data: any): Conversation => {
  // Map user1, user2, and otherParticipant from backend response
  const user1: User | undefined = data.user1 ? {
    id: data.user1.id,
    email: data.user1.email,
    displayName: data.user1.displayName,
  } : undefined;
  
  const user2: User | undefined = data.user2 ? {
    id: data.user2.id,
    email: data.user2.email,
    displayName: data.user2.displayName,
  } : undefined;
  
  const otherParticipant: User | undefined = data.otherParticipant ? {
    id: data.otherParticipant.id,
    email: data.otherParticipant.email,
    displayName: data.otherParticipant.displayName,
  } : undefined;

  // Build participants array from user1 and user2 if available
  const participants: User[] = [];
  if (user1) participants.push(user1);
  if (user2) participants.push(user2);

  const isGroup = data.isGroup || false;

  return {
    id: data.id,
    name: data.name,
    isGroup,
    groupId: isGroup ? data.id : undefined, // For groups, id IS the groupId
    participants: data.participants || participants,
    lastMessage: data.lastMessage,
    user1,
    user2,
    otherParticipant,
    createdAt: data.createdAt,
    lastMessageAt: data.lastMessageAt,
    unreadCount: data.unreadCount || 0,
  };
};

// Helper to map backend message response to frontend type
const mapMessage = (data: any): Message => ({
  id: data.id,
  content: data.content,
  senderId: data.senderId,
  senderName: data.senderDisplayName || data.senderUsername || data.senderEmail || 'Unknown',
  timestamp: data.createdAt || data.timestamp,
});

// Async thunks
export const fetchConversations = createAsyncThunk(
  'chat/fetchConversations',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get('/api/conversations');
      return response.data.map(mapConversation);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch conversations');
    }
  }
);

export const fetchMessages = createAsyncThunk(
  'chat/fetchMessages',
  async ({ conversationId, isGroup }: { conversationId: number; isGroup: boolean }, { rejectWithValue, getState }) => {
    try {
      // Pass isGroup to the backend to avoid ID collision issues
      const response = await api.get(`/api/conversations/${conversationId}/messages?isGroup=${isGroup}`);
      const state: any = getState();
      const user = state.auth.user;
      const conversation = state.chat.selectedConversation;
      
      console.log(`📥 Fetched ${response.data.length} messages for conversation ${conversationId} (isGroup=${isGroup})`);
      console.log(`📌 Conversation isGroup: ${conversation?.isGroup}`);
      
      // Decrypt messages if they are encrypted - use Promise.all for async operations
      return Promise.all(response.data.map(async (msg: any) => {
        // Check if message has ECDH encrypted fields (direct message encryption)
        const hasECDHFields = msg.encryptedContent && msg.encryptionNonce && msg.senderPublicKey;
        // Check if message has group encrypted fields (no senderPublicKey)
        const hasGroupFields = msg.encryptedContent && msg.encryptionNonce && !msg.senderPublicKey;
        
        // Use the message's isGroup flag from server (more reliable than conversation state)
        const isGroupMessage = msg.isGroup === true || (conversation?.isGroup && !msg.senderPublicKey);
        
        console.log(`Message ${msg.id} - hasECDHFields: ${hasECDHFields}, hasGroupFields: ${hasGroupFields}, isEncrypted: ${msg.isEncrypted}, isGroupMessage: ${isGroupMessage}`);
        
        // For GROUP messages (determined by message's isGroup flag)
        if (isGroupMessage) {
          console.log(`Message ${msg.id} is a GROUP message`);
          
          // If message has senderPublicKey, it was encrypted with ECDH (old method)
          // These messages cannot be decrypted with the group key
          if (hasECDHFields) {
            console.log(`Message ${msg.id} was encrypted with ECDH (legacy) - cannot decrypt with group key`);
            return mapMessage({
              ...msg,
              content: '[Legacy encrypted message - encrypted before group key was implemented]',
            });
          }
          
          // If message has group encrypted fields, decrypt with group key
          if (hasGroupFields) {
            console.log(`Message ${msg.id} - attempting group key decryption`);
            try {
              const groupKey = await KeyExchangeService.getGroupKey(conversation.id, user.id);
              if (!groupKey) {
                throw new Error('No group key available for decryption.');
              }

              const decryptedContent = EncryptionService.decryptWithGroupKey(
                {
                  ciphertext: msg.encryptedContent,
                  nonce: msg.encryptionNonce,
                },
                groupKey
              );

              return mapMessage({
                ...msg,
                content: decryptedContent,
              });
            } catch (error) {
              console.error(`❌ Failed to decrypt group message ${msg.id}:`, error);
              return mapMessage({
                ...msg,
                content: `[Group decryption failed: ${error instanceof Error ? error.message : String(error)}]`,
              });
            }
          }
          
          // No encrypted fields - use content as-is
          console.log(`Message ${msg.id} has no encrypted fields, using plaintext content`);
          return mapMessage(msg);
        }
        
        // For DIRECT conversations: Use ECDH-based decryption
        if (hasECDHFields) {
          try {
            console.log(`🔓 Attempting to decrypt message ${msg.id} from sender ${msg.senderId}`);
            const myPrivateKey = EncryptionService.getPrivateKey(user.id);
            
            if (!myPrivateKey) {
              console.warn(`⚠️ No private key found for user ${user.id}`);
              return mapMessage({
                ...msg,
                content: '[Cannot decrypt - no private key]',
              });
            }
            
            // Key derivation logic:
            // - If I sent this message (senderId == currentUser), derive shared secret using recipient's PUBLIC key
            // - If someone else sent it (senderId != currentUser), derive shared secret using sender's PUBLIC key
            const isMessageISent = msg.senderId === user.id;
            let publicKeyForDerivation = isMessageISent ? msg.recipientPublicKey : msg.senderPublicKey;
            
            // If we don't have recipientPublicKey in the message, get it from conversation
            if (isMessageISent && !publicKeyForDerivation && conversation) {
              const recipientUser = conversation.participants?.find((p: User) => p.id !== user.id);
              if (recipientUser) {
                // Fetch recipient's public key from KeyExchange service
                try {
                  const keyResp = await KeyExchangeService.getPublicKey(recipientUser.id);
                  publicKeyForDerivation = keyResp?.publicKey;
                } catch (e) {
                  console.warn('Failed to fetch recipient public key:', e);
                }
              }
            }
            
            console.log(`Message ${msg.id} is ${isMessageISent ? 'SENT' : 'RECEIVED'}`);
            console.log(`Using publicKey: ${publicKeyForDerivation?.substring(0, 20)}...`);
            console.log(`Deriving shared secret from myPrivateKey and ${isMessageISent ? 'recipientPublicKey' : 'senderPublicKey'}`);
            
            if (!publicKeyForDerivation) {
              throw new Error(`Missing ${isMessageISent ? 'recipient' : 'sender'} public key for decryption`);
            }
            
            const sharedSecret = EncryptionService.deriveSharedSecret(myPrivateKey, publicKeyForDerivation);
            console.log(`✅ Shared secret derived, attempting decryption`);
            
            const decryptedContent = EncryptionService.decryptMessage(
              {
                ciphertext: msg.encryptedContent,
                nonce: msg.encryptionNonce,
                senderPublicKey: msg.senderPublicKey,
              },
              sharedSecret
            );
            
            console.log(`✅ Message ${msg.id} decrypted successfully: "${decryptedContent}"`);
            return mapMessage({
              ...msg,
              content: decryptedContent,
            });
          } catch (error) {
            console.error(`❌ Failed to decrypt message ${msg.id}:`, error);
            return mapMessage({
              ...msg,
              content: `[Decryption failed: ${error instanceof Error ? error.message : String(error)}]`,
            });
          }
        }
        
        // No encrypted fields - use content as-is
        console.log(`Message ${msg.id} has no encrypted fields, using plaintext content`);
        return mapMessage(msg);
      }));
    } catch (error: any) {
      console.error('Error fetching messages:', error);
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch messages');
    }
  }
);

// Mark conversation as read
export const markConversationAsRead = createAsyncThunk(
  'chat/markAsRead',
  async (conversationId: number, { rejectWithValue }) => {
    try {
      await api.post(`/api/conversations/${conversationId}/read`);
      return conversationId;
    } catch (error: any) {
      console.error('Error marking conversation as read:', error);
      return rejectWithValue(error.response?.data?.message || 'Failed to mark as read');
    }
  }
);

export const sendMessage = createAsyncThunk(
  'chat/sendMessage',
  async (
    { conversationId, content }: { conversationId: number; content: string },
    { rejectWithValue, getState }
  ) => {
    try {
      const state: any = getState();

      const user = state.auth.user;
      if (!user) {
        return rejectWithValue('Not authenticated');
      }

      const conv = state.chat.selectedConversation;
      if (!conv) {
        return rejectWithValue('No conversation selected');
      }

      console.log('🔐 Starting encryption process...');
      console.log('Sender ID:', user.id);
      console.log('Conversation isGroup:', conv.isGroup);
      console.log('Message content:', content);

      let payload: any;
      let decryptionKey: any; // Will store the key/secret needed for decryption

      // ========================================
      // GROUP CHAT ENCRYPTION (Symmetric Key)
      // ========================================
      if (conv.isGroup) {
        console.log('📦 Using GROUP encryption (symmetric key)');
        
        // Get the group key
        const groupKey = await KeyExchangeService.getGroupKey(conv.id, user.id);
        if (!groupKey) {
          return rejectWithValue('❌ Could not retrieve group encryption key. Please try again.');
        }
        console.log('✅ Group key retrieved');

        // Encrypt message with group key
        const encryptedMsg = EncryptionService.encryptWithGroupKey(content, groupKey);
        console.log('✅ Message encrypted with group key');

        // Prepare payload for group message (no senderPublicKey needed)
        payload = {
          encryptedContent: encryptedMsg.ciphertext,
          encryptionNonce: encryptedMsg.nonce,
          isEncrypted: true,
          isGroupMessage: true, // Flag to indicate this is a group message
        };

        decryptionKey = groupKey;
      } 
      // ========================================
      // DIRECT CHAT ENCRYPTION (ECDH)
      // ========================================
      else {
        console.log('📦 Using DIRECT encryption (ECDH)');
        
        // Determine recipient id (other participant)
        const other = conv.otherParticipant || (conv.participants && conv.participants.find((p: any) => p.id !== user.id));
        if (!other) {
          return rejectWithValue('Conversation recipient not found');
        }
        const recipientId: number = other.id;
        console.log('Recipient ID:', recipientId);

        // Retrieve our private key from local storage (stored during initialization)
        const myPrivateKey = EncryptionService.getPrivateKey(user.id);
        if (!myPrivateKey) {
          return rejectWithValue('❌ Your encryption keys are not initialized. Please refresh the page and try again.');
        }
        console.log('✅ Private key retrieved');

        // Derive our actual public key from the private key
        const myPublicKey: string = EncryptionService.getPublicKeyFromPrivateKey(myPrivateKey);
        console.log('✅ My public key derived from private key');

        // Fetch recipient public key from server
        const recipientKeyResp = await KeyExchangeService.getPublicKey(recipientId);
        if (!recipientKeyResp) {
          return rejectWithValue(`❌ Recipient (${other.displayName}) has not initialized encryption yet. They must set up encryption first.`);
        }
        const recipientPublicKey: string = recipientKeyResp.publicKey;
        console.log('✅ Recipient public key retrieved');

        // Derive shared secret for this conversation
        const sharedSecret = EncryptionService.deriveSharedSecret(myPrivateKey, recipientPublicKey);
        console.log('✅ Shared secret derived');

        // Encrypt message
        const encryptedMsg = EncryptionService.encryptMessage(content, sharedSecret, myPublicKey);
        console.log('✅ Message encrypted');

        // Prepare request payload for direct message
        payload = {
          encryptedContent: encryptedMsg.ciphertext,
          encryptionNonce: encryptedMsg.nonce,
          senderPublicKey: encryptedMsg.senderPublicKey,
          isEncrypted: true,
          isGroupMessage: false, // Explicitly mark as direct message
        };

        decryptionKey = sharedSecret;

        // Cache shared secret locally for conversation
        EncryptionService.getOrCreateSharedSecret(conversationId, recipientId, myPrivateKey, recipientPublicKey);
      }

      console.log('📤 Sending message with payload:', payload);

      // Send encrypted message to server
      const response = await api.post(
        `/api/conversations/${conversationId}/messages`,
        payload
      );

      console.log('✅ Message sent successfully:', response.data);

      // Decrypt the message we just sent before storing it
      let decryptedMessage = response.data;
      
      if (conv.isGroup) {
        // Decrypt group message
        try {
          console.log('🔓 Decrypting sent group message...');
          const decryptedContent = EncryptionService.decryptWithGroupKey(
            {
              ciphertext: response.data.encryptedContent,
              nonce: response.data.encryptionNonce,
            },
            decryptionKey
          );
          console.log('✅ Sent group message decrypted successfully:', decryptedContent);
          decryptedMessage = {
            ...response.data,
            content: decryptedContent,
            isEncrypted: true,
          };
        } catch (decryptError) {
          console.error('❌ Failed to decrypt sent group message:', decryptError);
          decryptedMessage = {
            ...response.data,
            content: content, // Use original content since we just encrypted it
            isEncrypted: true,
          };
        }
      } else {
        // Decrypt direct message
        const hasEncryptedContent = response.data.encryptedContent && response.data.encryptionNonce && response.data.senderPublicKey;
        
        if (hasEncryptedContent) {
          try {
            console.log('🔓 Decrypting sent direct message...');
            const decryptedContent = EncryptionService.decryptMessage(
              {
                ciphertext: response.data.encryptedContent,
                nonce: response.data.encryptionNonce,
                senderPublicKey: response.data.senderPublicKey,
              },
              decryptionKey
            );
            console.log('✅ Sent direct message decrypted successfully:', decryptedContent);
            decryptedMessage = {
              ...response.data,
              content: decryptedContent,
              isEncrypted: true,
            };
          } catch (decryptError) {
            console.error('❌ Failed to decrypt sent message:', decryptError);
            decryptedMessage = {
              ...response.data,
              content: content, // Use original content
              isEncrypted: true,
            };
          }
        }
      }

      // Return mapped message with decrypted content
      const message = mapMessage(decryptedMessage);
      return message;
    } catch (error: any) {
      console.error('❌ Error sending message:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      
      const errorMessage = error.response?.data?.message || error.message || 'Failed to send message';
      return rejectWithValue(errorMessage);
    }
  }
);

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    selectConversation: (state, action: PayloadAction<Conversation | null>) => {
      state.selectedConversation = action.payload;
      state.messages = [];
    },
    setChatType: (state, action: PayloadAction<'direct' | 'group'>) => {
      state.chatType = action.payload;
      state.messages = [];
      state.selectedConversation = null;
    },
    addMessage: (state, action: PayloadAction<Message>) => {
      state.messages.push(action.payload);
    },
    clearChat: (state) => {
      state.conversations = [];
      state.selectedConversation = null;
      state.messages = [];
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch conversations
      .addCase(fetchConversations.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchConversations.fulfilled, (state, action) => {
        state.isLoading = false;
        state.conversations = action.payload;
      })
      .addCase(fetchConversations.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      // Fetch messages
      .addCase(fetchMessages.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchMessages.fulfilled, (state, action) => {
        state.isLoading = false;
        state.messages = action.payload;
      })
      .addCase(fetchMessages.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      // Send message
      .addCase(sendMessage.pending, (state) => {
        state.error = null;
      })
      .addCase(sendMessage.fulfilled, (state, action) => {
        state.messages.push(action.payload);
        state.error = null;
      })
      .addCase(sendMessage.rejected, (state, action) => {
        state.error = action.payload as string;
      })
      // Mark conversation as read
      .addCase(markConversationAsRead.fulfilled, (state, action) => {
        const conversationId = action.payload;
        // Reset unread count for this conversation
        const conversation = state.conversations.find(c => c.id === conversationId);
        if (conversation) {
          conversation.unreadCount = 0;
        }
      });
  },
});

export const { selectConversation, setChatType, addMessage, clearChat, clearError } = chatSlice.actions;
export default chatSlice.reducer;