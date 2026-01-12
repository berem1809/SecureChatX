import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import chatReducer from '../store/slices/chatSlice';
import requestReducer from './slices/requestSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    chat: chatReducer,
    requests: requestReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
