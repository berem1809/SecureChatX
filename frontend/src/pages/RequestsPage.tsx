import React, { useState, useEffect } from 'react';
import {
  Container,
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Avatar,
  Button,
  TextField,
  Alert,
  Tabs,
  Tab,
  Divider,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import GroupIcon from '@mui/icons-material/Group';
import ChatIcon from '@mui/icons-material/Chat';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
  fetchReceivedRequests,
  fetchSentRequests,
  sendChatRequest,
  acceptRequest,
  rejectRequest,
  clearMessages,
} from '../store/slices/requestSlice';
import api from '../services/api';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => (
  <div hidden={value !== index}>{value === index && <Box sx={{ p: 2 }}>{children}</Box>}</div>
);

const RequestsPage: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [email, setEmail] = useState('');
  const [requestType, setRequestType] = useState<'chat' | 'group'>('chat');
  const [groupName, setGroupName] = useState('');
  const [groupDialogOpen, setGroupDialogOpen] = useState(false);
  const [groupDescription, setGroupDescription] = useState('');
  const navigate = useNavigate();

  const dispatch = useAppDispatch();
  const { receivedRequests, sentRequests, isLoading, error, successMessage } = useAppSelector(
    (state) => state.requests
  );

  useEffect(() => {
    dispatch(fetchReceivedRequests());
    dispatch(fetchSentRequests());
  }, [dispatch]);

  useEffect(() => {
    return () => {
      dispatch(clearMessages());
    };
  }, [dispatch]);

  const handleSendRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    const result = await dispatch(sendChatRequest(email));
    if (sendChatRequest.fulfilled.match(result)) {
      setEmail('');
      dispatch(fetchSentRequests());
    }
  };

  const handleCreateGroup = async () => {
    try {
      await api.post('/api/groups', { name: groupName, description: groupDescription });
      setGroupDialogOpen(false);
      setGroupName('');
      setGroupDescription('');
      // Show success or navigate to groups
    } catch (err) {
      console.error('Failed to create group', err);
    }
  };

  const handleAccept = async (requestId: number) => {
    await dispatch(acceptRequest(requestId));
    // Refresh requests and navigate to chat
    dispatch(fetchReceivedRequests());
  };

  const handleReject = (requestId: number) => {
    dispatch(rejectRequest(requestId));
  };

  const handleStartChat = () => {
    navigate('/chat');
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'warning';
      case 'ACCEPTED':
        return 'success';
      case 'REJECTED':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Container maxWidth="md" sx={{ mt: 2 }}>
      {/* Send Request Form */}
      <Paper sx={{ p: 3, mb: 2 }}>
        <Typography variant="h6" gutterBottom>
          Send Chat Request
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {successMessage && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {successMessage}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSendRequest} sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <TextField
            sx={{ flex: 1, minWidth: 250 }}
            size="small"
            label="User Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Button type="submit" variant="contained" disabled={isLoading} startIcon={<ChatIcon />}>
            Send Request
          </Button>
          <Button 
            variant="outlined" 
            onClick={() => setGroupDialogOpen(true)}
            startIcon={<GroupIcon />}
          >
            Create Group
          </Button>
        </Box>
      </Paper>

      {/* Group Creation Dialog */}
      <Dialog open={groupDialogOpen} onClose={() => setGroupDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Group</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <TextField
              fullWidth
              label="Group Name"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
              required
              sx={{ mb: 2 }}
            />
            <TextField
              fullWidth
              label="Description (optional)"
              value={groupDescription}
              onChange={(e) => setGroupDescription(e.target.value)}
              multiline
              rows={3}
            />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
              You will be the admin of this group. You can invite members after creation.
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGroupDialogOpen(false)}>Cancel</Button>
          <Button 
            onClick={handleCreateGroup} 
            variant="contained" 
            disabled={!groupName.trim()}
          >
            Create Group
          </Button>
        </DialogActions>
      </Dialog>

      {/* Requests Tabs */}
      <Paper>
        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
          {/* Show total received count; pending list is shown inside the tab */}
          <Tab label={`Received (${useAppSelector((s) => s.requests.allReceivedRequests.length)})`} />
          <Tab label={`Sent (${sentRequests.length})`} />
        </Tabs>
        <Divider />

        <TabPanel value={tabValue} index={0}>
          <List>
            {useAppSelector((state) => state.requests.allReceivedRequests).length === 0 ? (
              <ListItem>
                <ListItemText primary="No requests received" />
              </ListItem>
            ) : (
              useAppSelector((state) => state.requests.allReceivedRequests).map((request) => {
                const senderName = request.sender?.displayName || request.senderName || 'Unknown User';
                const senderEmail = request.sender?.email || request.senderEmail || '';
                const isPending = request.status === 'PENDING';
                
                return (
                  <ListItem
                    key={request.id}
                    secondaryAction={
                      isPending ? (
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <Button
                            size="small"
                            variant="contained"
                            onClick={() => handleAccept(request.id)}
                          >
                            Accept
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            onClick={() => handleReject(request.id)}
                          >
                            Reject
                          </Button>
                        </Box>
                      ) : (
                        <Chip label={request.status} size="small" color={getStatusColor(request.status) as any} />
                      )
                    }
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: 'primary.main' }}>
                        <PersonIcon />
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={senderName}
                      secondary={
                        <Box component="span" sx={{ display: 'block' }}>
                          <Typography component="span" variant="body2" color="text.secondary" sx={{ display: 'block' }}>
                            {senderEmail}
                          </Typography>
                          <Typography component="span" variant="caption" color="text.secondary">
                            {new Date(request.createdAt).toLocaleDateString()} at {new Date(request.createdAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true })}
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                );
              })
            )}
          </List>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <List>
            {sentRequests.length === 0 ? (
              <ListItem>
                <ListItemText primary="No sent requests" />
              </ListItem>
            ) : (
              sentRequests.map((request) => {
                const receiverName = request.receiver?.displayName || request.receiverName || 'Unknown User';
                const receiverEmail = request.receiver?.email || request.receiverEmail || '';
                
                return (
                  <ListItem 
                    key={request.id}
                    secondaryAction={
                      request.status === 'ACCEPTED' ? (
                        <Button
                          size="small"
                          variant="contained"
                          onClick={handleStartChat}
                          startIcon={<ChatIcon />}
                        >
                          Start Chat
                        </Button>
                      ) : null
                    }
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: 'secondary.main' }}>
                        <PersonIcon />
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <span>{receiverName}</span>
                          <Chip 
                            label={request.status} 
                            size="small" 
                            color={getStatusColor(request.status) as any}
                          />
                        </Box>
                      }
                      secondary={
                        <Box component="span" sx={{ display: 'block' }}>
                          <Typography component="span" variant="body2" color="text.secondary" sx={{ display: 'block' }}>
                            {receiverEmail}
                          </Typography>
                          <Typography component="span" variant="caption" color="text.secondary">
                            {new Date(request.createdAt).toLocaleDateString()}
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                );
              })
            )}
          </List>
        </TabPanel>
      </Paper>
    </Container>
  );
};

export default RequestsPage;
