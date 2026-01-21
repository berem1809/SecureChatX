import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../services/api';
import { ChatRequest, GroupInvitation } from '../../types';

interface RequestState {
  receivedRequests: ChatRequest[];
  sentRequests: ChatRequest[];
  allReceivedRequests: ChatRequest[]; // includes all statuses
  groupInvitations: GroupInvitation[]; // pending group invitations
  isLoading: boolean;
  error: string | null;
  successMessage: string | null;
}

const initialState: RequestState = {
  receivedRequests: [],
  sentRequests: [],
  allReceivedRequests: [],
  groupInvitations: [],
  isLoading: false,
  error: null,
  successMessage: null,
};

// Helper to map backend response to frontend ChatRequest type
const mapChatRequest = (data: any): ChatRequest => ({
  id: data.id,
  senderId: data.sender?.id || data.senderId,
  senderName: data.sender?.displayName || data.senderName,
  senderEmail: data.sender?.email || data.senderEmail,
  receiverId: data.receiver?.id || data.receiverId,
  receiverName: data.receiver?.displayName || data.receiverName,
  receiverEmail: data.receiver?.email || data.receiverEmail,
  // Normalize status to uppercase to match type union
  status: (data.status || 'PENDING').toUpperCase() as ChatRequest['status'],
  createdAt: data.createdAt,
  updatedAt: data.updatedAt,
  sender: data.sender,
  receiver: data.receiver,
});

// Async thunks
export const fetchReceivedRequests = createAsyncThunk(
  'requests/fetchReceived',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get('/api/chat-requests/received');
      return response.data.map(mapChatRequest);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch requests');
    }
  }
);

export const fetchSentRequests = createAsyncThunk(
  'requests/fetchSent',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get('/api/chat-requests/sent');
      return response.data.map(mapChatRequest);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch requests');
    }
  }
);

export const sendChatRequest = createAsyncThunk(
  'requests/send',
  async (receiverEmail: string, { rejectWithValue }) => {
    try {
      await api.post('/api/chat-requests', { receiverEmail });
      return 'Chat request sent successfully!';
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to send request');
    }
  }
);

export const acceptRequest = createAsyncThunk(
  'requests/accept',
  async (requestId: number, { rejectWithValue }) => {
    try {
      await api.post(`/api/chat-requests/${requestId}/action`, { action: 'ACCEPT' });
      return requestId;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to accept request');
    }
  }
);

export const rejectRequest = createAsyncThunk(
  'requests/reject',
  async (requestId: number, { rejectWithValue }) => {
    try {
      await api.post(`/api/chat-requests/${requestId}/action`, { action: 'REJECT' });
      return requestId;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to reject request');
    }
  }
);

/**
 * Fetch pending group invitations
 */
export const fetchGroupInvitations = createAsyncThunk(
  'requests/fetchGroupInvitations',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get('/api/groups/invitations/pending');
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch group invitations');
    }
  }
);

/**
 * Accept a group invitation
 */
export const acceptGroupInvitation = createAsyncThunk(
  'requests/acceptGroupInvitation',
  async (invitationId: number, { rejectWithValue }) => {
    try {
      await api.post(`/api/groups/invitations/${invitationId}/action`, { action: 'ACCEPT' });
      return invitationId;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to accept invitation');
    }
  }
);

/**
 * Reject a group invitation
 */
export const rejectGroupInvitation = createAsyncThunk(
  'requests/rejectGroupInvitation',
  async (invitationId: number, { rejectWithValue }) => {
    try {
      await api.post(`/api/groups/invitations/${invitationId}/action`, { action: 'REJECT' });
      return invitationId;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to reject invitation');
    }
  }
);

/**
 * Create a new group
 * @param groupData Object with name and description (both required)
 */
export const createGroup = createAsyncThunk(
  'requests/createGroup',
  async (groupData: { name: string; description: string }, { rejectWithValue }) => {
    try {
      const response = await api.post('/api/groups', {
        name: groupData.name,
        description: groupData.description,
      });
      return {
        success: true,
        data: response.data,
        message: 'Group created successfully! You are now the admin.',
      };
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to create group';
      return rejectWithValue(message);
    }
  }
);

const requestSlice = createSlice({
  name: 'requests',
  initialState,
  reducers: {
    clearMessages: (state) => {
      state.error = null;
      state.successMessage = null;
    },
    clearRequests: (state) => {
      state.receivedRequests = [];
      state.sentRequests = [];
      state.allReceivedRequests = [];
      state.groupInvitations = [];
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch received
      .addCase(fetchReceivedRequests.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchReceivedRequests.fulfilled, (state, action) => {
        state.isLoading = false;
        state.allReceivedRequests = action.payload;
        state.receivedRequests = action.payload.filter((r: ChatRequest) => r.status === 'PENDING');
      })
      .addCase(fetchReceivedRequests.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      // Fetch sent
      .addCase(fetchSentRequests.fulfilled, (state, action) => {
        state.sentRequests = action.payload;
      })
      // Send request
      .addCase(sendChatRequest.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.successMessage = null;
      })
      .addCase(sendChatRequest.fulfilled, (state, action) => {
        state.isLoading = false;
        state.successMessage = action.payload;
      })
      .addCase(sendChatRequest.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      // Accept
      .addCase(acceptRequest.fulfilled, (state, action) => {
        state.receivedRequests = state.receivedRequests.filter((r) => r.id !== action.payload);
      })
      // Reject
      .addCase(rejectRequest.fulfilled, (state, action) => {
        state.receivedRequests = state.receivedRequests.filter((r) => r.id !== action.payload);
      })
      // Create group
      .addCase(createGroup.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.successMessage = null;
      })
      .addCase(createGroup.fulfilled, (state, action) => {
        state.isLoading = false;
        state.successMessage = action.payload.message;
      })
      .addCase(createGroup.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      // Fetch group invitations
      .addCase(fetchGroupInvitations.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchGroupInvitations.fulfilled, (state, action) => {
        state.isLoading = false;
        state.groupInvitations = action.payload;
      })
      .addCase(fetchGroupInvitations.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      // Accept group invitation
      .addCase(acceptGroupInvitation.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(acceptGroupInvitation.fulfilled, (state, action) => {
        state.isLoading = false;
        state.groupInvitations = state.groupInvitations.filter(
          (inv) => inv.id !== action.payload
        );
        state.successMessage = 'Group invitation accepted!';
      })
      .addCase(acceptGroupInvitation.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      // Reject group invitation
      .addCase(rejectGroupInvitation.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(rejectGroupInvitation.fulfilled, (state, action) => {
        state.isLoading = false;
        state.groupInvitations = state.groupInvitations.filter(
          (inv) => inv.id !== action.payload
        );
        state.successMessage = 'Group invitation rejected';
      })
      .addCase(rejectGroupInvitation.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });
  },
});

export const { clearMessages, clearRequests } = requestSlice.actions;
export default requestSlice.reducer;
