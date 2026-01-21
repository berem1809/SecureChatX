import React, { useState, useEffect, useMemo } from 'react';
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
  Divider,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  Alert,
  Tabs,
  Tab,
} from '@mui/material';
import GroupIcon from '@mui/icons-material/Group';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import PersonIcon from '@mui/icons-material/Person';
import ExitToAppIcon from '@mui/icons-material/ExitToApp';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import { useNavigate } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';
import api from '../services/api';

/**
 * ============================================================================
 * GROUPS PAGE - Manage group chats and memberships
 * ============================================================================
 * 
 * FEATURES:
 * ---------
 * 1. View all groups where user is a member
 * 2. Display group details and member list
 * 3. Admin-only controls:
 *    - Invite new members by email
 *    - Promote members to admin
 *    - Remove members from group
 * 4. Member controls:
 *    - Leave group (with restrictions for admin)
 * 5. Group deletion:
 *    - Auto-deleted when admin leaves and is the last member
 *    - All memberships and messages are cleared
 * 
 * ADMIN RULES:
 * -----------
 * - Admin can invite members (searches by email)
 * - Admin can promote members to admin
 * - Admin can remove members from group
 * - Admin CANNOT leave if other members exist (must promote someone first)
 * - Admin can leave if they're the last member (group is deleted)
 * 
 * MEMBER RULES:
 * -----------
 * - Members can view group info and member list
 * - Members can leave group at any time
 * - Members cannot invite or remove other members
 * - Members cannot promote other members
 */

interface GroupMember {
  id: number;
  userId: number;
  displayName: string;
  email: string;
  role: 'ADMIN' | 'MEMBER';
  joinedAt: string;
}

interface Group {
  id: number;
  name: string;
  description: string;
  createdBy: {
    id: number;
    displayName: string;
    email: string;
  };
  memberCount: number;
  members?: GroupMember[];
  createdAt: string;
  role: 'ADMIN' | 'MEMBER';
}

const GroupsPage: React.FC = () => {
  const [tabValue, setTabValue] = useState(0); // 0 = My Groups, 1 = Discover Groups
  const [groups, setGroups] = useState<Group[]>([]);
  const [allGroups, setAllGroups] = useState<Group[]>([]); // All available groups
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [memberDialogOpen, setMemberDialogOpen] = useState(false);
  const [joinRequestDialogOpen, setJoinRequestDialogOpen] = useState(false);
  const [joinConfirmDialogOpen, setJoinConfirmDialogOpen] = useState(false);
  const [selectedGroupForRequest, setSelectedGroupForRequest] = useState<Group | null>(null);
  const [requestMessage, setRequestMessage] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [groupCreatorFriends, setGroupCreatorFriends] = useState<Set<number>>(new Set());
  const navigate = useNavigate();
  const currentUserId = useAppSelector((state) => state.auth.user?.id);

  // Determine admin capability using group role or membership list (fallback when API omits role)
  const isAdminForSelectedGroup = useMemo(() => {
    if (!selectedGroup || !currentUserId) return false;
    if (selectedGroup.role === 'ADMIN') return true;
    return members.some((m) => m.userId === currentUserId && m.role === 'ADMIN');
  }, [selectedGroup, currentUserId, members]);

  useEffect(() => {
    fetchGroups();
  }, []);

  useEffect(() => {
    if (tabValue === 1) {
      fetchAllGroups();
    }
  }, [tabValue]);

  const fetchGroups = async () => {
    try {
      setIsLoading(true);
      const response = await api.get('/api/groups');
      setGroups(response.data);
      setError('');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch groups');
    } finally {
      setIsLoading(false);
    }
  };

  const fetchAllGroups = async () => {
    try {
      setIsLoading(true);
      // For now, we'll get user's groups - in a real app, you'd have a /api/groups/all endpoint
      // or search functionality. Since we only have user's groups, we'll show them here too
      const response = await api.get('/api/groups');
      const userGroupIds = new Set(response.data.map((g: Group) => g.id));
      
      // Filter to show only groups where user is not a member (for demonstration)
      // In production, you'd fetch from /api/groups/public or similar
      setAllGroups(response.data);
      
      // Check which group creators are friends
      await checkGroupCreatorFriendships(response.data);
      
      setError('');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch groups');
    } finally {
      setIsLoading(false);
    }
  };

  const checkGroupCreatorFriendships = async (groups: Group[]) => {
    try {
      const friends = new Set<number>();
      
      // Check each group creator for existing conversation
      await Promise.all(
        groups.map(async (group) => {
          try {
            const response = await api.get(`/api/conversations/exists/${group.createdBy.id}`);
            if (response.data === true) {
              friends.add(group.createdBy.id);
            }
          } catch (err) {
            console.debug(`Could not check friendship with user ${group.createdBy.id}`);
          }
        })
      );
      
      setGroupCreatorFriends(friends);
    } catch (err) {
      console.error('Error checking group creator friendships:', err);
    }
  };

  const fetchGroupMembers = async (groupId: number) => {
    try {
      const response = await api.get(`/api/groups/${groupId}/members`);
      setMembers(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch members');
    }
  };

  const handleSelectGroup = async (group: Group) => {
    setSelectedGroup(group);
    await fetchGroupMembers(group.id);
  };

  /**
   * Invite a user to the group (admin only)
   */
  const handleInviteMember = async () => {
    if (!inviteEmail.trim() || !selectedGroup) return;

    try {
      setIsLoading(true);
      
      // First, search for user by email to get their ID
      const searchResponse = await api.get(`/api/users/search/email?q=${inviteEmail}`);
      if (searchResponse.data.length === 0) {
        setError('User not found');
        return;
      }
      
      const invitedUserId = searchResponse.data[0].id;
      
      // Send invitation
      await api.post(`/api/groups/${selectedGroup.id}/invitations`, {
        inviteeId: invitedUserId,
      });
      
      setSuccessMessage(`Invitation sent to ${inviteEmail}`);
      setInviteEmail('');
      setMemberDialogOpen(false);
      
      // Refresh members
      await fetchGroupMembers(selectedGroup.id);
      
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to send invitation');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Remove a member from the group (admin only)
   */
  const handleRemoveMember = async (memberId: number, memberName: string) => {
    if (!selectedGroup) return;
    
    if (!window.confirm(`Are you sure you want to remove ${memberName} from the group?`)) {
      return;
    }

    try {
      setIsLoading(true);
      await api.delete(`/api/groups/${selectedGroup.id}/members/${memberId}`);
      
      setSuccessMessage(`${memberName} removed from group`);
      await fetchGroupMembers(selectedGroup.id);
      
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to remove member');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Promote a member to admin (admin only)
   */
  const handlePromoteMember = async (memberId: number, memberName: string) => {
    if (!selectedGroup) return;
    
    if (!window.confirm(`Promote ${memberName} to admin?`)) {
      return;
    }

    try {
      setIsLoading(true);
      await api.post(`/api/groups/${selectedGroup.id}/members/${memberId}/promote`);
      
      setSuccessMessage(`${memberName} promoted to admin`);
      await fetchGroupMembers(selectedGroup.id);
      
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to promote member');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Leave the group
   */
  const handleLeaveGroup = async (group: Group) => {
    if (!window.confirm(`Are you sure you want to leave "${group.name}"?`)) {
      return;
    }

    try {
      setIsLoading(true);
      await api.post(`/api/groups/${group.id}/leave`);
      
      setSuccessMessage('You left the group');
      setSelectedGroup(null);
      await fetchGroups();
      
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to leave group');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Open dialog to request joining a group
   */
  const handleOpenJoinRequest = (group: Group) => {
    setSelectedGroupForRequest(group);
    
    // If the group creator is a friend, show confirmation dialog
    if (groupCreatorFriends.has(group.createdBy.id)) {
      setJoinConfirmDialogOpen(true);
    } else {
      // Otherwise, show the regular request message dialog
      setRequestMessage(`Hi! I would like to join "${group.name}". ${group.description}`);
      setJoinRequestDialogOpen(true);
    }
  };

  /**
   * Confirm joining a friend-created group
   */
  const handleConfirmJoinGroup = () => {
    if (!selectedGroupForRequest) return;
    
    setJoinConfirmDialogOpen(false);
    setRequestMessage(`Hi! I would like to join "${selectedGroupForRequest.name}". ${selectedGroupForRequest.description}`);
    setJoinRequestDialogOpen(true);
  };

  /**
   * Send join request to group admin
   */
  const handleSendJoinRequest = async () => {
    if (!selectedGroupForRequest || !requestMessage.trim()) return;

    try {
      setIsLoading(true);
      
      // Send request to group creator/admin
      // This creates a chat request to the group admin with context about the group
      await api.post('/api/chat-requests', {
        receiverEmail: selectedGroupForRequest.createdBy.email,
        message: requestMessage,
      });
      
      setSuccessMessage(`Join request sent to ${selectedGroupForRequest.createdBy.displayName}`);
      setJoinRequestDialogOpen(false);
      setRequestMessage('');
      setSelectedGroupForRequest(null);
      
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to send join request');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 2, mb: 2 }}>
      {error && (
        <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {successMessage && (
        <Alert severity="success" onClose={() => setSuccessMessage('')} sx={{ mb: 2 }}>
          {successMessage}
        </Alert>
      )}

      <Paper sx={{ display: 'flex', height: 'calc(100vh - 180px)' }}>
        {/* Groups List with Tabs */}
        <Box sx={{ width: { xs: '100%', sm: 350 }, borderRight: 1, borderColor: 'divider', display: 'flex', flexDirection: 'column' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} variant="fullWidth">
            <Tab label="My Groups" />
            <Tab label="Discover" />
          </Tabs>
          <Divider />
          
          {/* Tab 0: My Groups */}
          {tabValue === 0 && (
            <List sx={{ overflow: 'auto', flex: 1 }}>
              {isLoading && groups.length === 0 ? (
                <ListItem>
                  <ListItemText primary="Loading..." />
                </ListItem>
              ) : groups.length === 0 ? (
                <ListItem>
                  <ListItemText 
                    primary="No groups yet" 
                    secondary="Create a group from the Requests page or discover groups"
                  />
                </ListItem>
              ) : (
                groups.map((group) => (
                  <ListItemButton
                    key={group.id}
                    selected={selectedGroup?.id === group.id}
                    onClick={() => handleSelectGroup(group)}
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: 'primary.main' }}>
                        <GroupIcon />
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={group.name}
                      secondary={
                        <Typography component="span" variant="body2" color="text.secondary">
                          {group.memberCount} member{group.memberCount !== 1 ? 's' : ''}
                        </Typography>
                      }
                    />
                    <Chip
                      size="small"
                      label={group.role}
                      icon={group.role === 'ADMIN' ? <AdminPanelSettingsIcon /> : <PersonIcon />}
                      variant="outlined"
                      sx={{ mt: 0.5 }}
                    />
                  </ListItemButton>
                ))
              )}
            </List>
          )}

          {/* Tab 1: Discover Groups */}
          {tabValue === 1 && (
            <List sx={{ overflow: 'auto', flex: 1 }}>
              {isLoading && allGroups.length === 0 ? (
                <ListItem>
                  <ListItemText primary="Loading..." />
                </ListItem>
              ) : allGroups.length === 0 ? (
                <ListItem>
                  <ListItemText 
                    primary="No groups available" 
                    secondary="Check back later or create your own group"
                  />
                </ListItem>
              ) : (
                allGroups.map((group) => {
                  const isMember = groups.some(g => g.id === group.id);
                  return (
                    <ListItem key={group.id}>
                      <ListItemAvatar>
                        <Avatar sx={{ bgcolor: 'secondary.main' }}>
                          <GroupIcon />
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={group.name}
                        secondary={
                          <>
                            <Typography component="span" variant="body2" color="text.secondary" display="block">
                              {group.description}
                            </Typography>
                            <Typography component="span" variant="caption" color="text.secondary">
                              {group.memberCount} member{group.memberCount !== 1 ? 's' : ''} • By {group.createdBy.displayName}
                            </Typography>
                          </>
                        }
                      />
                      {isMember ? (
                        <Chip label="Joined" color="success" size="small" />
                      ) : (
                        <Button
                          variant="contained"
                          size="small"
                          startIcon={<SendIcon />}
                          onClick={() => handleOpenJoinRequest(group)}
                          disabled={isLoading}
                        >
                          Request to Join
                        </Button>
                      )}
                    </ListItem>
                  );
                })
              )}
            </List>
          )}
        </Box>

        {/* Group Details */}
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          {selectedGroup ? (
            <>
              {/* Group Header */}
              <Box sx={{ p: 3, borderBottom: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                  <Box>
                    <Typography variant="h5" gutterBottom>
                      {selectedGroup.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      {selectedGroup.description}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Created by {selectedGroup.createdBy.displayName} on{' '}
                      {new Date(selectedGroup.createdAt).toLocaleDateString()}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    {isAdminForSelectedGroup && (
                      <Button
                        variant="contained"
                        size="small"
                        onClick={() => setMemberDialogOpen(true)}
                      >
                        Invite Member
                      </Button>
                    )}
                    <Button
                      variant="outlined"
                      size="small"
                      startIcon={<ExitToAppIcon />}
                      onClick={() => handleLeaveGroup(selectedGroup)}
                      disabled={isLoading}
                    >
                      Leave
                    </Button>
                  </Box>
                </Box>
              </Box>

              {/* Members List */}
              <Box sx={{ overflow: 'auto', flex: 1 }}>
                <Typography variant="subtitle1" sx={{ p: 2 }}>
                  Members ({members.length})
                </Typography>
                <List>
                  {members.map((member) => (
                    <ListItem
                      key={member.id}
                      secondaryAction={
                        isAdminForSelectedGroup && member.role !== 'ADMIN' ? (
                          <Box sx={{ display: 'flex', gap: 1 }}>
                            <Button
                              size="small"
                              variant="outlined"
                              onClick={() =>
                                handlePromoteMember(member.userId, member.displayName)
                              }
                            >
                              Promote
                            </Button>
                            <Button
                              size="small"
                              color="error"
                              variant="outlined"
                              startIcon={<DeleteIcon />}
                              onClick={() =>
                                handleRemoveMember(member.userId, member.displayName)
                              }
                            >
                              Remove
                            </Button>
                          </Box>
                        ) : (
                          <Chip
                            size="small"
                            label={member.role}
                            icon={<AdminPanelSettingsIcon />}
                            variant="outlined"
                          />
                        )
                      }
                    >
                      <ListItemAvatar>
                        <Avatar sx={{ bgcolor: 'secondary.main' }}>
                          <PersonIcon />
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={member.displayName}
                        secondary={
                          <>
                            <Typography component="span" variant="body2" color="text.secondary">
                              {member.email}
                            </Typography>
                            <br />
                            <Typography component="span" variant="caption" color="text.secondary">
                              Joined {new Date(member.joinedAt).toLocaleDateString()}
                            </Typography>
                          </>
                        }
                      />
                    </ListItem>
                  ))}
                </List>
              </Box>
            </>
          ) : (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
              }}
            >
              <Typography color="text.secondary">Select a group to view details</Typography>
            </Box>
          )}
        </Box>
      </Paper>

      {/* Invite Member Dialog */}
      <Dialog open={memberDialogOpen} onClose={() => setMemberDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Invite Member</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <TextField
              fullWidth
              label="User Email"
              type="email"
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              placeholder="Enter user email to invite"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMemberDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleInviteMember}
            variant="contained"
            disabled={!inviteEmail.trim() || isLoading}
          >
            Send Invitation
          </Button>
        </DialogActions>
      </Dialog>

      {/* Join Confirmation Dialog (for friend-created groups) */}
      <Dialog 
        open={joinConfirmDialogOpen} 
        onClose={() => {
          setJoinConfirmDialogOpen(false);
          setSelectedGroupForRequest(null);
        }} 
        maxWidth="sm" 
        fullWidth
      >
        <DialogTitle>Join Group</DialogTitle>
        <DialogContent>
          {selectedGroupForRequest && (
            <Box sx={{ mt: 2 }}>
              <Alert severity="info" sx={{ mb: 3 }}>
                This group was created by your friend {selectedGroupForRequest.createdBy.displayName}!
              </Alert>
              <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                  {selectedGroupForRequest.name}
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  {selectedGroupForRequest.description}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Created by: {selectedGroupForRequest.createdBy.displayName}
                </Typography>
                <br />
                <Typography variant="caption" color="text.secondary">
                  Members: {selectedGroupForRequest.memberCount}
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 3 }}>
                Would you like to send a request to join this group?
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setJoinConfirmDialogOpen(false);
            setSelectedGroupForRequest(null);
          }}>
            Cancel
          </Button>
          <Button
            onClick={handleConfirmJoinGroup}
            variant="contained"
            color="primary"
          >
            Yes, Send Request
          </Button>
        </DialogActions>
      </Dialog>

      {/* Join Request Dialog */}
      <Dialog open={joinRequestDialogOpen} onClose={() => setJoinRequestDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Request to Join Group</DialogTitle>
        <DialogContent>
          {selectedGroupForRequest && (
            <Box sx={{ mt: 2 }}>
              <Box sx={{ mb: 3, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                  {selectedGroupForRequest.name}
                </Typography>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  {selectedGroupForRequest.description}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Admin: {selectedGroupForRequest.createdBy.displayName}
                </Typography>
              </Box>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Your Message"
                value={requestMessage}
                onChange={(e) => setRequestMessage(e.target.value)}
                placeholder="Introduce yourself and explain why you'd like to join this group"
                helperText="Your request will be sent to the group admin for approval"
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setJoinRequestDialogOpen(false);
            setRequestMessage('');
            setSelectedGroupForRequest(null);
          }}>
            Cancel
          </Button>
          <Button
            onClick={handleSendJoinRequest}
            variant="contained"
            startIcon={<SendIcon />}
            disabled={!requestMessage.trim() || isLoading}
          >
            Send Request
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default GroupsPage;
