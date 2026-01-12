import React, { useState, useEffect } from 'react';
import {
  Container,
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  Button,
  TextField,
  Alert,
  Tabs,
  Tab,
  Divider,
} from '@mui/material';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
  fetchReceivedRequests,
  fetchSentRequests,
  sendChatRequest,
  acceptRequest,
  rejectRequest,
  clearMessages,
} from '../store/slices/requestSlice';

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

  const handleAccept = (requestId: number) => {
    dispatch(acceptRequest(requestId));
  };

  const handleReject = (requestId: number) => {
    dispatch(rejectRequest(requestId));
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

        <Box component="form" onSubmit={handleSendRequest} sx={{ display: 'flex', gap: 2 }}>
          <TextField
            fullWidth
            size="small"
            label="User Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Button type="submit" variant="contained" disabled={isLoading}>
            Send
          </Button>
        </Box>
      </Paper>

      {/* Requests Tabs */}
      <Paper>
        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
          <Tab label={`Received (${receivedRequests.length})`} />
          <Tab label={`Sent (${sentRequests.length})`} />
        </Tabs>
        <Divider />

        <TabPanel value={tabValue} index={0}>
          <List>
            {receivedRequests.length === 0 ? (
              <ListItem>
                <ListItemText primary="No pending requests" />
              </ListItem>
            ) : (
              receivedRequests.map((request) => (
                <ListItem
                  key={request.id}
                  secondaryAction={
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
                  }
                >
                  <ListItemText
                    primary={request.senderName}
                    secondary={new Date(request.createdAt).toLocaleDateString()}
                  />
                </ListItem>
              ))
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
              sentRequests.map((request) => (
                <ListItem key={request.id}>
                  <ListItemText
                    primary={`To: User #${request.receiverId}`}
                    secondary={`Status: ${request.status} - ${new Date(
                      request.createdAt
                    ).toLocaleDateString()}`}
                  />
                </ListItem>
              ))
            )}
          </List>
        </TabPanel>
      </Paper>
    </Container>
  );
};

export default RequestsPage;
