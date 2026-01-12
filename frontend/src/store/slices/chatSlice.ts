import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import api from '../../services/api';
import { Conversation, Message, User } from '../../types';

interface ChatState {
  conversations: Conversation[];
  selectedConversation: Conversation | null;
  messages: Message[];
  isLoading: boolean;
  error: string | null;
}

const initialState: ChatState = {
  conversations: [],
  selectedConversation: null,
  messages: [],
  isLoading: false,
  error: null,
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

  return {
    id: data.id,
    name: data.name,
    isGroup: data.isGroup || false,
    participants: data.participants || participants,
    lastMessage: data.lastMessage,
    user1,
    user2,
    otherParticipant,
    createdAt: data.createdAt,
    lastMessageAt: data.lastMessageAt,
  };
};

// Helper to map backend message response to frontend type
const mapMessage = (data: any): Message => ({
  id: data.id,
  content: data.content,
  senderId: data.senderId,
  senderName: data.senderName || 'Unknown',
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
  async (conversationId: number, { rejectWithValue }) => {
    try {
      const response = await api.get(`/api/conversations/${conversationId}/messages`);
      return response.data.map(mapMessage);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch messages');
    }
  }
);

export const sendMessage = createAsyncThunk(
  'chat/sendMessage',
  async ({ conversationId, content }: { conversationId: number; content: string }, { rejectWithValue }) => {
    try {
      const response = await api.post(`/api/conversations/${conversationId}/messages`, { content });
      return mapMessage(response.data);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to send message');
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
    addMessage: (state, action: PayloadAction<Message>) => {
      state.messages.push(action.payload);
    },
    clearChat: (state) => {
      state.conversations = [];
      state.selectedConversation = null;
      state.messages = [];
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
      .addCase(sendMessage.fulfilled, (state, action) => {
        state.messages.push(action.payload);
      });
  },
});

export const { selectConversation, addMessage, clearChat } = chatSlice.actions;
export default chatSlice.reducer;
