import React from 'react';
import { Container, Box, Typography, Button, Paper } from '@mui/material';
import Grid from '@mui/material/Grid';
import { Link } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';
import ChatIcon from '@mui/icons-material/Chat';
import SendIcon from '@mui/icons-material/Send';

const HomePage: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4 }}>
        {/* Welcome Section */}
        <Paper sx={{ p: 4, mb: 4, bgcolor: '#1a1a2e', color: 'white' }}>
          <Typography variant="h4" gutterBottom>
            Welcome back, {user?.displayName || 'User'}!
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.9 }}>
            Ready to chat? Select an option below to get started.
          </Typography>
        </Paper>

        {/* Quick Actions */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Paper
              sx={{
                p: 4,
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
                transition: 'transform 0.2s',
                '&:hover': { transform: 'translateY(-4px)', boxShadow: 4 },
              }}
            >
              <ChatIcon sx={{ fontSize: 60, color: '#1a1a2e', mb: 2 }} />
              <Typography variant="h5" gutterBottom>
                My Conversations
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3, flexGrow: 1 }}>
                View and continue your existing conversations with friends and contacts.
              </Typography>
              <Button
                variant="contained"
                size="large"
                component={Link}
                to="/chat"
                startIcon={<ChatIcon />}
                sx={{
                  bgcolor: '#ffc107',
                  color: '#000',
                  '&:hover': { bgcolor: '#ffca28' },
                }}
              >
                Go to Chat
              </Button>
            </Paper>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Paper
              sx={{
                p: 4,
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
                transition: 'transform 0.2s',
                '&:hover': { transform: 'translateY(-4px)', boxShadow: 4 },
              }}
            >
              <SendIcon sx={{ fontSize: 60, color: '#1a1a2e', mb: 2 }} />
              <Typography variant="h5" gutterBottom>
                Chat Requests
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3, flexGrow: 1 }}>
                Send new chat requests or manage pending requests from other users.
              </Typography>
              <Button
                variant="outlined"
                size="large"
                component={Link}
                to="/requests"
                startIcon={<SendIcon />}
                sx={{
                  borderColor: '#1a1a2e',
                  color: '#1a1a2e',
                  '&:hover': { borderColor: '#ffc107', bgcolor: '#ffc107', color: '#000' },
                }}
              >
                Chat Requests
              </Button>
            </Paper>
          </Grid>
        </Grid>

        {/* Tips Section */}
        <Paper sx={{ p: 3, mt: 4 }}>
          <Typography variant="h6" gutterBottom>
            Quick Tips
          </Typography>
          <Typography variant="body2" color="text.secondary">
            • Send a chat request to connect with new people
          </Typography>
          <Typography variant="body2" color="text.secondary">
            • Accept incoming requests to start conversations
          </Typography>
          <Typography variant="body2" color="text.secondary">
            • Messages are delivered in real-time
          </Typography>
        </Paper>
      </Box>
    </Container>
  );
};

export default HomePage;
