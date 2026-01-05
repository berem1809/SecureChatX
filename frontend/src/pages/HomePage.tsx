import React from 'react';
import { Container, Box, Typography, Button, Paper } from '@mui/material';
import { Link } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';

const HomePage: React.FC = () => {
  const { isAuthenticated } = useAppSelector((state) => state.auth);

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 8, textAlign: 'center' }}>
        <Typography variant="h3" component="h1" gutterBottom>
          Welcome to ChatApp
        </Typography>
        <Typography variant="h6" color="text.secondary" paragraph>
          A simple real-time chat application
        </Typography>

        <Paper sx={{ p: 4, mt: 4 }}>
          {isAuthenticated ? (
            <>
              <Typography variant="body1" paragraph>
                You are logged in. Start chatting!
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
                <Button variant="contained" component={Link} to="/chat">
                  Go to Chat
                </Button>
                <Button variant="outlined" component={Link} to="/requests">
                  Chat Requests
                </Button>
              </Box>
            </>
          ) : (
            <>
              <Typography variant="body1" paragraph>
                Please login or register to start chatting.
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
                <Button variant="contained" component={Link} to="/login">
                  Login
                </Button>
                <Button variant="outlined" component={Link} to="/register">
                  Register
                </Button>
              </Box>
            </>
          )}
        </Paper>

        <Box sx={{ mt: 4 }}>
          <Typography variant="body2" color="text.secondary">
            Features:
          </Typography>
          <Typography variant="body2" color="text.secondary">
            • Send and receive chat requests
          </Typography>
          <Typography variant="body2" color="text.secondary">
            • Real-time messaging
          </Typography>
          <Typography variant="body2" color="text.secondary">
            • Secure authentication with JWT
          </Typography>
        </Box>
      </Box>
    </Container>
  );
};

export default HomePage;
