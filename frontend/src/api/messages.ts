import api from "./axios";
import type { Message } from "../types";

export interface MessageListResponse {
  messages: Message[];
  hasMore: boolean;
  nextCursor: string | null;
}

export interface SendMessagePayload {
  type: "text" | "image" | "file" | "voice";
  content?: string;
  attachmentUrl?: string;
  replyToMessageId?: string;
}

export async function getMessages(
  conversationId: string,
  params?: { before?: string; after?: string; limit?: number },
): Promise<MessageListResponse> {
  const res = await api.get<MessageListResponse>(
    `/conversations/${conversationId}/messages`,
    { params },
  );
  return res.data;
}

export async function sendMessage(
  conversationId: string,
  payload: SendMessagePayload,
): Promise<Message> {
  const res = await api.post<Message>(
    `/conversations/${conversationId}/messages`,
    payload,
  );
  return res.data;
}

export async function editMessage(
  messageId: string,
  content: string,
): Promise<Message> {
  const res = await api.patch<Message>(`/messages/${messageId}`, { content });
  return res.data;
}

export async function deleteMessage(messageId: string): Promise<Message> {
  const res = await api.delete<Message>(`/messages/${messageId}`);
  return res.data;
}

export async function markMessageRead(messageId: string): Promise<void> {
  await api.patch(`/messages/${messageId}/status`, { status: "read" });
}

export async function replyToMessage(
  messageId: string,
  payload: SendMessagePayload,
): Promise<Message> {
  const res = await api.post<Message>(`/messages/${messageId}/reply`, payload);
  return res.data;
}

export interface UploadUrlRequest {
  fileName: string;
  fileType: string;
  conversationId: string;
}

export interface UploadUrlResponse {
  uploadUrl: string;
  fileKey: string;
  expiresIn: number;
}

export async function getUploadUrl(
  payload: UploadUrlRequest,
): Promise<UploadUrlResponse> {
  const res = await api.post<UploadUrlResponse>("/messages/upload", payload);
  return res.data;
}
