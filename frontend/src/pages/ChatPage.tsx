import React, { useState, useEffect, useRef } from 'react';
import {
  Container,
  Box,
  Paper,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  TextField,
  IconButton,
  Typography,
  Divider,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
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
              conversations.map((conv) => (
                <ListItemButton
                  key={conv.id}
                  selected={selectedConversation?.id === conv.id}
                  onClick={() => dispatch(selectConversation(conv))}
                >
                  <ListItemText
                    primary={conv.name || conv.participants.map((p) => p.displayName).join(', ')}
                    secondary={conv.lastMessage?.content || 'No messages'}
                  />
                </ListItemButton>
              ))
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
                <Typography variant="h6">
                  {selectedConversation.name ||
                    selectedConversation.participants.map((p) => p.displayName).join(', ')}
                </Typography>
              </Box>

              {/* Messages */}
              <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
                {messages.map((msg) => (
                  <Box
                    key={msg.id}
                    sx={{
                      mb: 1,
                      p: 1,
                      borderRadius: 1,
                      bgcolor: 'grey.100',
                      maxWidth: '70%',
                    }}
                  >
                    <Typography variant="caption" color="text.secondary">
                      {msg.senderName}
                    </Typography>
                    <Typography variant="body1">{msg.content}</Typography>
                  </Box>
                ))}
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
