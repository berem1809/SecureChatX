import React, { useEffect, useState } from 'react';
import { AppBar, Toolbar, Typography, Button, Box, Container, Badge } from '@mui/material';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/slices/authSlice';
import { clearChat } from '../store/slices/chatSlice';
import { clearRequests } from '../store/slices/requestSlice';
import api from '../services/api';

interface NotificationCounts {
  unreadDirectMessages: number;
  unreadGroupMessages: number;
  pendingChatRequests: number;
  pendingGroupInvitations: number;
  totalNotifications: number;
}

const Navbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const navigate = useNavigate();
  const location = useLocation();
  const [notificationCounts, setNotificationCounts] = useState<NotificationCounts>({
    unreadDirectMessages: 0,
    unreadGroupMessages: 0,
    pendingChatRequests: 0,
    pendingGroupInvitations: 0,
    totalNotifications: 0,
  });

  // Fetch notification counts on mount and periodically
  useEffect(() => {
    if (!user) return;

    const fetchNotifications = async () => {
      try {
        const response = await api.get('/api/notifications/counts');
        setNotificationCounts(response.data);
      } catch (error) {
        console.error('Failed to fetch notification counts:', error);
      }
    };

    // Initial fetch
    fetchNotifications();

    // Poll every 30 seconds for updates
    const interval = setInterval(fetchNotifications, 30000);

    return () => clearInterval(interval);
  }, [user]);

  const handleLogout = async () => {
    await dispatch(logout());
    dispatch(clearChat());
    dispatch(clearRequests());
    navigate('/');
  };

  const isActive = (path: string) => {
    return location.pathname === path;
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
            <Button 
              component={Link} 
              to="/home" 
              sx={{ 
                color: 'white',
                borderBottom: isActive('/home') ? '3px solid #ffc107' : 'none',
                pb: 0.5
              }}
            >
              HOME
            </Button>
            <Button 
              component={Link} 
              to="/chat" 
              sx={{ 
                color: 'white',
                borderBottom: isActive('/chat') ? '3px solid #ffc107' : 'none',
                pb: 0.5
              }}
            >
              CHAT
            </Button>
            <Button 
              component={Link} 
              to="/users" 
              sx={{ 
                color: 'white',
                borderBottom: isActive('/users') ? '3px solid #ffc107' : 'none',
                pb: 0.5
              }}
            >
              USERS
            </Button>
            <Badge 
              badgeContent={notificationCounts.pendingChatRequests} 
              color="error"
              sx={{ '& .MuiBadge-badge': { top: 8, right: -10 } }}
            >
              <Button 
                component={Link} 
                to="/requests" 
                sx={{ 
                  color: 'white',
                  borderBottom: isActive('/requests') ? '3px solid #ffc107' : 'none',
                  pb: 0.5
                }}
              >
                REQUESTS
              </Button>
            </Badge>
            <Button 
              component={Link} 
              to="/groups" 
              sx={{ 
                color: 'white',
                borderBottom: isActive('/groups') ? '3px solid #ffc107' : 'none',
                pb: 0.5
              }}
            >
              GROUPS
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
