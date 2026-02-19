export interface User {
  id: string;
  username: string;
  email: string;
  displayName?: string | null;
  avatarUrl?: string | null;
  lastSeen?: string | null;
  createdAt: string;
  updatedAt?: string;
}

export interface AuthResponse {
  user: User;
  token: string;
  refreshToken: string;
}

export interface LastMessage {
  id: string;
  content?: string | null;
  senderId: string;
  senderName?: string | null;
  createdAt: string;
  type: string;
}

export interface Conversation {
  id: string;
  type: "direct" | "group";
  name?: string | null;
  avatarUrl?: string | null;
  lastMessage?: LastMessage | null;
  participants: Participant[];
  unreadCount?: number;
  isMuted?: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface Participant {
  userId: string;
  displayName: string;
  avatarUrl?: string | null;
  isAdmin?: boolean;
}

export interface UserSearchResult {
  id: string;
  username: string;
  displayName?: string | null;
  avatarUrl?: string | null;
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  sender?: {
    displayName: string;
    avatarUrl?: string | null;
  };
  content: string;
  type: "text" | "image" | "file" | "voice";
  attachmentUrl?: string | null;
  replyTo?: {
    messageId: string;
    content: string;
    senderId: string;
  } | null;
  isEdited?: boolean;
  isDeleted?: boolean;
  createdAt: string;
  updatedAt?: string;
}
