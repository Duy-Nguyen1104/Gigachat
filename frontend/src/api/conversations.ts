import api from "./axios";
import type { Conversation } from "../types";

export interface ConversationListResponse {
  conversations: Conversation[];
  total: number;
  hasMore: boolean;
}

export async function getConversations(
  page = 0,
  size = 50,
): Promise<ConversationListResponse> {
  const res = await api.get<ConversationListResponse>("/conversations", {
    params: { page, size },
  });
  return res.data;
}

export async function getConversation(id: string): Promise<Conversation> {
  const res = await api.get<Conversation>(`/conversations/${id}`);
  return res.data;
}

export async function createDirectConversation(
  participantId: string,
): Promise<Conversation> {
  const res = await api.post<Conversation>("/conversations", {
    type: "direct",
    participantId,
  });
  return res.data;
}

export async function markConversationRead(id: string): Promise<void> {
  await api.patch(`/conversations/${id}/read`);
}

export async function createGroupConversation(
  name: string,
  participantIds: string[],
): Promise<Conversation> {
  const res = await api.post<Conversation>("/conversations", {
    type: "group",
    name,
    participantIds,
  });
  return res.data;
}
