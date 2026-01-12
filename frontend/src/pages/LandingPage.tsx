import React from 'react';
import { Container, Box, Typography, Button, Paper } from '@mui/material';
import { Link, Navigate } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';
import ChatIcon from '@mui/icons-material/Chat';
import SecurityIcon from '@mui/icons-material/Security';
import GroupIcon from '@mui/icons-material/Group';

const LandingPage: React.FC = () => {
  const { isAuthenticated } = useAppSelector((state) => state.auth);

  // If already authenticated, show a message or link to home, but don't redirect
  // For now, we'll allow authenticated users to see the landing page

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#f5f5f5' }}>
      {/* Hero Section */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
          color: 'white',
          py: 12,
          textAlign: 'center',
        }}
      >
        <Container maxWidth="md">
          <Typography variant="h2" component="h1" fontWeight="bold" gutterBottom>
            Welcome to ChatApp
          </Typography>
          <Typography variant="h5" sx={{ mb: 4, opacity: 0.9 }}>
            Connect with friends and colleagues in real-time
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
            <Button
              variant="contained"
              size="large"
              component={Link}
              to="/login"
              sx={{
                bgcolor: '#ffc107',
                color: '#000',
                '&:hover': { bgcolor: '#ffca28' },
                px: 4,
                py: 1.5,
              }}
            >
              Login
            </Button>
            <Button
              variant="outlined"
              size="large"
              component={Link}
              to="/register"
              sx={{
                borderColor: 'white',
                color: 'white',
                '&:hover': { borderColor: '#ffc107', color: '#ffc107' },
                px: 4,
                py: 1.5,
              }}
            >
              Register
            </Button>
          </Box>
        </Container>
      </Box>

      {/* Features Section */}
      <Container maxWidth="lg" sx={{ py: 8 }}>
        <Typography variant="h4" textAlign="center" gutterBottom fontWeight="bold">
          Features
        </Typography>
        <Typography variant="body1" textAlign="center" color="text.secondary" sx={{ mb: 6 }}>
          Everything you need for seamless communication
        </Typography>

        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 4 }}>
          <Paper sx={{ p: 4, textAlign: 'center', height: '100%' }}>
            <ChatIcon sx={{ fontSize: 60, color: '#1a1a2e', mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              Real-time Messaging
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Send and receive messages instantly with WebSocket technology
            </Typography>
          </Paper>

          <Paper sx={{ p: 4, textAlign: 'center', height: '100%' }}>
            <GroupIcon sx={{ fontSize: 60, color: '#1a1a2e', mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              Chat Requests
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Send and accept chat requests to connect with new people
            </Typography>
          </Paper>

          <Paper sx={{ p: 4, textAlign: 'center', height: '100%' }}>
            <SecurityIcon sx={{ fontSize: 60, color: '#1a1a2e', mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              Secure Authentication
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Your data is protected with JWT-based authentication
            </Typography>
          </Paper>
        </Box>
      </Container>

      {/* Footer */}
      <Box sx={{ bgcolor: '#1a1a2e', color: 'white', py: 4, textAlign: 'center' }}>
        <Typography variant="body2">
          © 2026 ChatApp. All rights reserved.
        </Typography>
      </Box>
    </Box>
  );
};

export default LandingPage;
