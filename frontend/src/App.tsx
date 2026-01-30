import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { Provider } from 'react-redux';
import { store } from './store';
import { useAppDispatch, useAppSelector } from './store/hooks';
import { fetchCurrentUser } from './store/slices/authSlice';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import LandingPage from './pages/LandingPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ChatPage from './pages/ChatPage';
import RequestsPage from './pages/RequestsPage';
import GroupsPage from './pages/GroupsPage';
import UsersPage from './pages/UsersPage';
import EncryptionService from './services/encryption';
import KeyExchangeService from './services/keyExchange';

// Simple theme with no colors (grayscale)
const theme = createTheme({
  palette: {
    primary: {
      main: '#424242',
    },
    secondary: {
      main: '#757575',
    },
  },
});

// Component to handle user fetching and encryption initialization
const AppContent: React.FC = () => {
  const dispatch = useAppDispatch();
  const { isAuthenticated, user, token } = useAppSelector((state) => state.auth);

  useEffect(() => {
    // Fetch user info if authenticated and have token but user not loaded
    if (isAuthenticated && token && !user) {
      dispatch(fetchCurrentUser());
    }
  }, [isAuthenticated, token, user, dispatch]);

  // Initialize encryption after user is loaded
  useEffect(() => {
    if (user && user.id) {
      initializeUserEncryption(user.id);
    }
  }, [user]);

  const initializeUserEncryption = async (userId: number) => {
    try {
      // Check if user already has a public key
      const hasKey = await KeyExchangeService.hasPublicKey(userId);
      
      if (!hasKey) {
        console.log('🔐 Initializing encryption for user:', userId);
        // Generate keypair, store private key, upload public key
        await KeyExchangeService.initializeEncryption(
          EncryptionService.generateKeyPair,
          EncryptionService.storePrivateKey,
          userId
        );
      } else {
        console.log('✅ Encryption already initialized for user:', userId);
        // Verify private key is stored locally
        const privateKey = EncryptionService.getPrivateKey(userId);
        if (!privateKey) {
          console.warn('⚠️ Private key missing! User may need to re-initialize encryption.');
        }
      }
    } catch (error) {
      console.error('Failed to initialize encryption:', error);
      // Non-fatal error - app can still function, but user won't be able to send encrypted messages
    }
  };

  return (
    <BrowserRouter>
      <Routes>
        {/* Public route - Landing Page (no navbar) */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        
        {/* Protected routes with Navbar */}
        <Route
          path="/home"
          element={
            <ProtectedRoute>
              <>
                <Navbar />
                <HomePage />
              </>
            </ProtectedRoute>
          }
        />
        <Route
          path="/chat"
          element={
            <ProtectedRoute>
              <>
                <Navbar />
                <ChatPage />
              </>
            </ProtectedRoute>
          }
        />
        <Route
          path="/requests"
          element={
            <ProtectedRoute>
              <>
                <Navbar />
                <RequestsPage />
              </>
            </ProtectedRoute>
          }
        />
        <Route
          path="/groups"
          element={
            <ProtectedRoute>
              <>
                <Navbar />
                <GroupsPage />
              </>
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute>
              <>
                <Navbar />
                <UsersPage />
              </>
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
};

const App: React.FC = () => {
  return (
    <Provider store={store}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <AppContent />
      </ThemeProvider>
    </Provider>
  );
};

export default App;
