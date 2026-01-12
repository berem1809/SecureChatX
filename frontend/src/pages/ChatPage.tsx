import React, { useState, useEffect, useRef } from 'react';
import {
  Container,
  Box,
  Paper,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemAvatar,
  Avatar,
  TextField,
  IconButton,
  Typography,
  Divider,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import PersonIcon from '@mui/icons-material/Person';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
  fetchConversations,
  fetchMessages,
  sendMessage,
  selectConversation,
} from '../store/slices/chatSlice';

const ChatPage: React.FC = () => {
  const [newMessage, setNewMessage] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const dispatch = useAppDispatch();
  const { conversations, selectedConversation, messages, isLoading } = useAppSelector(
    (state) => state.chat
  );
  const currentUser = useAppSelector((state) => state.auth.user);

  useEffect(() => {
    dispatch(fetchConversations());
  }, [dispatch]);

  useEffect(() => {
    if (selectedConversation) {
      dispatch(fetchMessages(selectedConversation.id));
    }
  }, [selectedConversation, dispatch]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedConversation) return;

    dispatch(sendMessage({ conversationId: selectedConversation.id, content: newMessage }));
    setNewMessage('');
  };

  // Helpers for time/date formatting and separators
  const formatTime = (timestamp: string) => {
    const d = new Date(timestamp);
    return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
  };

  const formatDate = (timestamp: string) => {
    const d = new Date(timestamp);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const isNewDateBlock = (prevTs?: string, currTs?: string) => {
    if (!prevTs || !currTs) return true;
    return formatDate(prevTs) !== formatDate(currTs);
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 2 }}>
      <Paper sx={{ height: 'calc(100vh - 120px)', display: 'flex' }}>
        {/* Conversation List */}
        <Box sx={{ width: { xs: '100%', sm: 300 }, borderRight: 1, borderColor: 'divider' }}>
          <Typography variant="h6" sx={{ p: 2 }}>
            Conversations
          </Typography>
          <Divider />
          <List sx={{ overflow: 'auto', height: 'calc(100% - 60px)' }}>
            {isLoading ? (
              <ListItem>
                <ListItemText primary="Loading..." />
              </ListItem>
            ) : conversations.length === 0 ? (
              <ListItem>
                <ListItemText primary="No conversations yet" secondary="Send a chat request to start" />
              </ListItem>
            ) : (
              conversations.map((conv) => {
                // Get the other participant's info
                const otherUser = conv.otherParticipant || 
                  (conv.participants && conv.participants.length > 0 
                    ? conv.participants[0] 
                    : null);
                const displayName = otherUser?.displayName || conv.name || 'Unknown User';
                const email = otherUser?.email || '';
                
                return (
                  <ListItemButton
                    key={conv.id}
                    selected={selectedConversation?.id === conv.id}
                    onClick={() => dispatch(selectConversation(conv))}
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: 'primary.main' }}>
                        <PersonIcon />
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={displayName}
                      secondary={
                        <Box component="span" sx={{ display: 'flex', flexDirection: 'column' }}>
                          <Typography variant="caption" color="text.secondary" noWrap>
                            {email}
                          </Typography>
                          {conv.lastMessage && (
                            <Typography variant="caption" color="text.secondary" noWrap>
                              {conv.lastMessage.content}
                            </Typography>
                          )}
                        </Box>
                      }
                    />
                  </ListItemButton>
                );
              })
            )}
          </List>
        </Box>

        {/* Chat Area */}
        <Box
          sx={{
            flex: 1,
            display: { xs: selectedConversation ? 'flex' : 'none', sm: 'flex' },
            flexDirection: 'column',
          }}
        >
          {selectedConversation ? (
            <>
              {/* Chat Header */}
              <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                {(() => {
                  const otherUser = selectedConversation.otherParticipant || 
                    (selectedConversation.participants && selectedConversation.participants.length > 0 
                      ? selectedConversation.participants[0] 
                      : null);
                  const displayName = otherUser?.displayName || selectedConversation.name || 'Unknown User';
                  const email = otherUser?.email || '';
                  return (
                    <>
                      <Typography variant="h6">{displayName}</Typography>
                      {email && (
                        <Typography variant="caption" color="text.secondary">
                          {email}
                        </Typography>
                      )}
                    </>
                  );
                })()}
              </Box>

              {/* Messages */}
              <Box sx={{ flex: 1, overflow: 'auto', p: 2, display: 'flex', flexDirection: 'column' }}>
                {messages.map((msg, idx) => {
                  const prev = idx > 0 ? messages[idx - 1] : undefined;
                  const showDate = isNewDateBlock(prev?.timestamp, msg.timestamp);
                  const isSelf = currentUser?.id === msg.senderId;
                  return (
                    <React.Fragment key={msg.id}>
                      {showDate && (
                        <Box sx={{ display: 'flex', justifyContent: 'center', my: 1 }}>
                          <Typography
                            variant="caption"
                            sx={{
                              bgcolor: 'grey.300',
                              color: 'grey.700',
                              px: 1.5,
                              py: 0.5,
                              borderRadius: 2,
                            }}
                          >
                            {formatDate(msg.timestamp)}
                          </Typography>
                        </Box>
                      )}

                      <Box sx={{ display: 'flex', justifyContent: isSelf ? 'flex-end' : 'flex-start', mb: 1 }}>
                        <Box
                          sx={{
                            maxWidth: '70%',
                            bgcolor: isSelf ? '#DCF8C6' : '#EDEDED',
                            color: 'text.primary',
                            px: 1.5,
                            py: 1,
                            borderRadius: 2,
                            boxShadow: '0 1px 2px rgba(0,0,0,0.08)',
                            borderBottomLeftRadius: isSelf ? 16 : 4,
                            borderBottomRightRadius: isSelf ? 4 : 16,
                          }}
                        >
                          <Typography variant="caption" sx={{ color: isSelf ? 'primary.main' : 'secondary.main', fontWeight: 600 }}>
                            {isSelf ? 'You' : msg.senderName}
                          </Typography>
                          <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                            {msg.content}
                          </Typography>
                          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 0.5 }}>
                            <Typography variant="caption" sx={{ color: 'grey.600', fontSize: '0.75rem' }}>
                              {formatTime(msg.timestamp)}
                            </Typography>
                          </Box>
                        </Box>
                      </Box>
                    </React.Fragment>
                  );
                })}
                <div ref={messagesEndRef} />
              </Box>

              {/* Message Input */}
              <Box
                component="form"
                onSubmit={handleSendMessage}
                sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1 }}
              >
                <TextField
                  fullWidth
                  size="small"
                  placeholder="Type a message..."
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                />
                <IconButton type="submit" disabled={!newMessage.trim()}>
                  <SendIcon />
                </IconButton>
              </Box>
            </>
          ) : (
            <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Typography color="text.secondary">Select a conversation to start chatting</Typography>
            </Box>
          )}
        </Box>
      </Paper>
    </Container>
  );
};

export default ChatPage;
