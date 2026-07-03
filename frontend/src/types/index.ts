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

export type RepoStatus =
  | "PENDING"
  | "CLONING"
  | "ANALYZING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"
  | string;

export interface RepositoryUser {
  id: string;
  email: string;
  createdAt?: string | null;
}

export interface RepositoryAnalysis {
  id: string;
  summary?: string | null;
  techStack?: string | null;
  createdAt?: string | null;
  architecturePattern?: string | null;
  projectStructure?: string | null;
  generatedReadme?: string | null;
  totalFiles?: number | null;
  totalSizeKb?: number | null;
  resumeBullets?: string | null;
  interviewQuestions?: string | null;
}

export interface Repository {
  id: string;
  githubUrl: string;
  status: RepoStatus;
  createdAt: string | null;
  analysis: RepositoryAnalysis | null;
  user: RepositoryUser | null;
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
