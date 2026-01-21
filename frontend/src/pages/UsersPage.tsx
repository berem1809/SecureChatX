import React, { useState, useEffect } from 'react';
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
  Typography,
  Button,
  CircularProgress,
  Alert,
  TextField,
  InputAdornment,
  Chip,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import SendIcon from '@mui/icons-material/Send';
import SearchIcon from '@mui/icons-material/Search';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { sendChatRequest, clearMessages } from '../store/slices/requestSlice';
import api from '../services/api';

interface UserInfo {
  id: number;
  email: string;
  displayName: string;
  profilePicture?: string;
}

const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<UserInfo[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sentRequests, setSentRequests] = useState<Set<number>>(new Set());
  const [existingFriends, setExistingFriends] = useState<Set<number>>(new Set());
  const [successMessage, setSuccessMessage] = useState('');
  const navigate = useNavigate();

  const dispatch = useAppDispatch();
  const { error: requestError, successMessage: requestSuccess } = useAppSelector(
    (state) => state.requests
  );

  useEffect(() => {
    fetchAllUsers();
  }, []);

  useEffect(() => {
    return () => {
      dispatch(clearMessages());
    };
  }, [dispatch]);

  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(''), 3000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  const fetchAllUsers = async () => {
    try {
      setIsLoading(true);
      setError('');
      const response = await api.get<UserInfo[]>('/api/users/all');
      setUsers(response.data);
      setFilteredUsers(response.data);
      
      // Check for existing conversations (friendships)
      await checkExistingFriendships(response.data);
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to fetch users';
      setError(errorMessage);
      console.error('Error fetching users:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const checkExistingFriendships = async (users: UserInfo[]) => {
    try {
      const friends = new Set<number>();
      
      // Check each user for existing conversation
      await Promise.all(
        users.map(async (user) => {
          try {
            const response = await api.get(`/api/conversations/exists/${user.id}`);
            if (response.data === true) {
              friends.add(user.id);
            }
          } catch (err) {
            // Ignore errors for individual checks
            console.debug(`Could not check friendship with user ${user.id}`);
          }
        })
      );
      
      setExistingFriends(friends);
    } catch (err) {
      console.error('Error checking friendships:', err);
    }
  };

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    if (!query.trim()) {
      setFilteredUsers(users);
    } else {
      const lowerQuery = query.toLowerCase();
      setFilteredUsers(
        users.filter(
          (user) =>
            user.email.toLowerCase().includes(lowerQuery) ||
            user.displayName.toLowerCase().includes(lowerQuery)
        )
      );
    }
  };

  const handleSendRequest = async (userId: number, userEmail: string) => {
    try {
      const result = await dispatch(sendChatRequest(userEmail));
      if (sendChatRequest.fulfilled.match(result)) {
        setSentRequests((prev) => new Set(prev).add(userId));
        setSuccessMessage(`Friend request sent to ${userEmail}!`);
      }
    } catch (err) {
      console.error('Error sending request:', err);
    }
  };

  const getInitials = (displayName: string) => {
    return displayName
      .split(' ')
      .map((name) => name[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  };

  const stringToColor = (string: string) => {
    let hash = 0;
    let i;
    for (i = 0; i < string.length; i += 1) {
      hash = string.charCodeAt(i) + ((hash << 5) - hash);
    }
    let color = '#';
    for (i = 0; i < 3; i += 1) {
      const value = (hash >> (i * 8)) & 0xff;
      color += `00${value.toString(16)}`.slice(-2);
    }
    return color;
  };

  return (
    <Container maxWidth="md" sx={{ mt: 2, mb: 4 }}>
      {/* Header */}
      <Paper sx={{ p: 3, mb: 3, backgroundColor: '#f5f5f5' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <PersonIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            Available Users
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary">
          Find and connect with other users. Send a friend request to start chatting!
        </Typography>
      </Paper>

      {/* Alerts */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {requestError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => dispatch(clearMessages())}>
          {requestError}
        </Alert>
      )}
      {successMessage && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {successMessage}
        </Alert>
      )}

      {/* Search Bar */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <TextField
          fullWidth
          placeholder="Search by email or name..."
          value={searchQuery}
          onChange={(e) => handleSearch(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          size="small"
        />
      </Paper>

      {/* Users List */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      ) : filteredUsers.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="body1" color="text.secondary">
            {searchQuery ? 'No users found matching your search' : 'No users available'}
          </Typography>
        </Paper>
      ) : (
        <Paper>
          <List>
            {filteredUsers.map((user, index) => (
              <React.Fragment key={user.id}>
                <ListItem
                  secondaryAction={
                    <Button
                      variant={
                        existingFriends.has(user.id) || sentRequests.has(user.id)
                          ? 'outlined'
                          : 'contained'
                      }
                      size="small"
                      startIcon={
                        existingFriends.has(user.id) ? (
                          <CheckCircleIcon />
                        ) : sentRequests.has(user.id) ? (
                          <CheckCircleIcon />
                        ) : (
                          <SendIcon />
                        )
                      }
                      onClick={() => handleSendRequest(user.id, user.email)}
                      disabled={existingFriends.has(user.id) || sentRequests.has(user.id)}
                      sx={{
                        backgroundColor:
                          existingFriends.has(user.id) || sentRequests.has(user.id)
                            ? 'transparent'
                            : 'primary.main',
                        color:
                          existingFriends.has(user.id) || sentRequests.has(user.id)
                            ? 'primary.main'
                            : 'white',
                      }}
                    >
                      {existingFriends.has(user.id)
                        ? 'Friends'
                        : sentRequests.has(user.id)
                        ? 'Sent'
                        : 'Add Friend'}
                    </Button>
                  }
                  disablePadding
                  sx={{ pl: 2, pr: 2 }}
                >
                  <ListItemButton
                    disabled
                    sx={{
                      cursor: 'default',
                      '&.Mui-disabled': {
                        opacity: 1,
                      },
                    }}
                  >
                    <ListItemAvatar>
                      <Avatar
                        sx={{
                          backgroundColor: stringToColor(user.displayName),
                          color: 'white',
                          fontWeight: 'bold',
                        }}
                      >
                        {getInitials(user.displayName)}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Typography variant="subtitle1" sx={{ fontWeight: 500 }}>
                          {user.displayName}
                        </Typography>
                      }
                      secondary={
                        <Typography variant="body2" color="text.secondary">
                          {user.email}
                        </Typography>
                      }
                    />
                  </ListItemButton>
                </ListItem>
                {index < filteredUsers.length - 1 && <Box sx={{ borderBottom: '1px solid #e0e0e0' }} />}
              </React.Fragment>
            ))}
          </List>
        </Paper>
      )}

      {/* Info Box */}
      <Paper sx={{ p: 3, mt: 3, backgroundColor: '#e3f2fd' }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          <strong>How it works:</strong>
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          • Click "Add Friend" to send a friend request to any user
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          • The user will see your request in their Requests tab
        </Typography>
        <Typography variant="body2" color="text.secondary">
          • Once they accept, you can start chatting directly!
        </Typography>
      </Paper>
    </Container>
  );
};

export default UsersPage;
