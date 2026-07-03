import { apiClient } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "@/types";

export const authApi = {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>("/api/v1/auth/login", payload);
    return data;
  },
  async register(payload: RegisterRequest): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>("/api/v1/auth/register", payload);
    return data;
  },
};
