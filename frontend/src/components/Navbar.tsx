import React from 'react';
import { AppBar, Toolbar, Typography, Button, Box, Container } from '@mui/material';
import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/slices/authSlice';
import { clearChat } from '../store/slices/chatSlice';
import { clearRequests } from '../store/slices/requestSlice';

const Navbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await dispatch(logout());
    dispatch(clearChat());
    dispatch(clearRequests());
    navigate('/');
  };

  return (
    <AppBar position="static" sx={{ bgcolor: '#1a1a2e' }}>
      <Container maxWidth="lg">
        <Toolbar disableGutters>
          <Typography
            variant="h6"
            component={Link}
            to="/home"
            sx={{ textDecoration: 'none', color: 'inherit', flexGrow: 1, fontWeight: 'bold' }}
          >
            ChatApp
          </Typography>

          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <Button component={Link} to="/home" sx={{ color: 'white' }}>
              Home
            </Button>
            <Button component={Link} to="/chat" sx={{ color: 'white' }}>
              Chat
            </Button>
            <Button component={Link} to="/users" sx={{ color: 'white' }}>
              Users
            </Button>
            <Button component={Link} to="/requests" sx={{ color: 'white' }}>
              Requests
            </Button>
            <Button component={Link} to="/groups" sx={{ color: 'white' }}>
              Groups
            </Button>
            <Typography variant="body2" sx={{ mx: 2, color: 'rgba(255,255,255,0.7)' }}>
              {user?.displayName}
            </Typography>
            <Button
              onClick={handleLogout}
              sx={{
                bgcolor: '#ffc107',
                color: '#000',
                '&:hover': { bgcolor: '#ffca28' },
              }}
            >
              Logout
            </Button>
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
};

export default Navbar;
