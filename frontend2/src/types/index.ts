export interface AuthResponse {
  token: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface AnalyzeRepoRequest {
  githubUrl: string;
}

export type RepoStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | string;

export interface Repository {
  id: string;
  name?: string;
  status?: RepoStatus;
  source?: "GITHUB" | "ZIP" | string;
  githubUrl?: string;
  createdAt?: string;
  uploadedAt?: string;
  lastOpenedAt?: string;
  // Allow arbitrary fields returned by backend
  [key: string]: unknown;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  createdAt: number;
}

export interface User {
  email: string;
}
