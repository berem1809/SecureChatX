import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../services/api';
import { ChatRequest } from '../../types';

interface RequestState {
  receivedRequests: ChatRequest[];
  sentRequests: ChatRequest[];
  allReceivedRequests: ChatRequest[]; // includes all statuses
  isLoading: boolean;
  error: string | null;
  successMessage: string | null;
}

const initialState: RequestState = {
  receivedRequests: [],
  sentRequests: [],
  allReceivedRequests: [],
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
      });
  },
});

export const { clearMessages, clearRequests } = requestSlice.actions;
export default requestSlice.reducer;
