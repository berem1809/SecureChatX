// User types
export interface User {
  id: number;
  email: string;
  displayName: string;
}

// Auth types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  user?: User;
}

// Message types
export interface Message {
  id: number;
  content: string;
  senderId: number;
  senderName: string;
  timestamp: string;
}

// Conversation types
export interface Conversation {
  id: number;
  name?: string;
  isGroup: boolean;
  participants: User[];
  lastMessage?: Message;
}

// Chat Request types
export interface ChatRequest {
  id: number;
  senderId: number;
  senderName: string;
  receiverId: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}
