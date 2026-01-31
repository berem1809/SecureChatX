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
  userId?: number;
  email?: string;
  displayName?: string;
  user?: User;
}

// Message types
export interface Message {
  id: number;
  content: string;
  senderId: number;
  senderName: string;
  timestamp: string;
  // Encryption fields
  isEncrypted?: boolean;
  ciphertext?: string;
  nonce?: string;
  senderPublicKey?: string;
  encryptedContent?: string;
  encryptionNonce?: string;
  recipientPublicKey?: string;
  recipientId?: number;
}

// Conversation types - matches backend ConversationResponse
export interface Conversation {
  id: number;
  name?: string;
  isGroup: boolean;
  groupId?: number; // For group conversations, this equals id
  participants: User[];
  lastMessage?: Message;
  // New fields matching backend ConversationResponse
  user1?: User;
  user2?: User;
  otherParticipant?: User;
  createdAt?: string;
  lastMessageAt?: string;
  unreadCount?: number; // Number of unread messages for current user
}

// Chat Request types - matches backend ChatRequestResponse
export interface ChatRequest {
  id: number;
  senderId: number;
  senderName: string;
  senderEmail?: string;
  receiverId: number;
  receiverName?: string;
  receiverEmail?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
  updatedAt?: string;
  // Full user objects from backend
  sender?: User;
  receiver?: User;
}

// Group types
export interface GroupMember {
  id: number;
  userId: number;
  email: string;
  displayName: string;
  role: 'ADMIN' | 'MEMBER';
  joinedAt: string;
  encryptedGroupKey?: string;
  keyNonce?: string;
  senderPublicKey?: string;
}

export interface Group {
  id: number;
  name: string;
  description?: string;
  createdBy: {
    id: number;
    displayName: string;
    email: string;
  };
  createdAt: string;
  memberCount: number;
  members?: GroupMember[];
  role?: 'ADMIN' | 'MEMBER';
}

export interface GroupInvitation {
  id: number;
  groupId: number;
  groupName: string;
  inviterId: number;
  inviterName: string;
  inviteeId: number;
  inviteeName: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}
