import api from "./axios";
import type { User } from "../types";

export interface UpdateProfileRequest {
  displayName?: string;
  email?: string;
}

export interface AvatarUploadRequest {
  fileName: string;
  fileType: string;
}

export interface AvatarUploadResponse {
  uploadUrl: string;
  viewUrl: string;
  expiresInMinutes: number;
}

export async function getProfile(): Promise<User> {
  const res = await api.get<User>("/user/me");
  return res.data;
}

export async function updateProfile(
  payload: UpdateProfileRequest,
): Promise<User> {
  const res = await api.patch<User>("/user/me", payload);
  return res.data;
}

export async function getAvatarUploadUrl(
  payload: AvatarUploadRequest,
): Promise<AvatarUploadResponse> {
  const res = await api.post<AvatarUploadResponse>("/user/avatar", payload);
  return res.data;
}
