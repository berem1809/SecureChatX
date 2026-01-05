import React from 'react';
import { AppBar, Toolbar, Typography, Button, Box, Container } from '@mui/material';
import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/slices/authSlice';
import { clearChat } from '../store/slices/chatSlice';
import { clearRequests } from '../store/slices/requestSlice';

const Navbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const { isAuthenticated } = useAppSelector((state) => state.auth);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await dispatch(logout());
    dispatch(clearChat());
    dispatch(clearRequests());
    navigate('/login');
  };

  return (
    <AppBar position="static" color="default" elevation={1}>
      <Container maxWidth="lg">
        <Toolbar disableGutters>
          <Typography
            variant="h6"
            component={Link}
            to="/"
            sx={{ textDecoration: 'none', color: 'inherit', flexGrow: 1 }}
          >
            ChatApp
          </Typography>

          <Box sx={{ display: 'flex', gap: 1 }}>
            {isAuthenticated ? (
              <>
                <Button component={Link} to="/chat">
                  Chat
                </Button>
                <Button component={Link} to="/requests">
                  Requests
                </Button>
                <Button onClick={handleLogout}>
                  Logout
                </Button>
              </>
            ) : (
              <>
                <Button component={Link} to="/login">
                  Login
                </Button>
                <Button component={Link} to="/register">
                  Register
                </Button>
              </>
            )}
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
};

export default Navbar;
