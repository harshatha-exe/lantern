import { apiClient } from "./client";
import type { AnalyzeRepoRequest, Repository } from "@/types";

export const repositoriesApi = {
  async submitGithub(payload: AnalyzeRepoRequest): Promise<Repository> {
    const { data } = await apiClient.post<Repository>("/api/v1/repositories", payload);
    return data;
  },
  async uploadZip(file: File): Promise<Repository> {
    const formData = new FormData();
    formData.append("file", file);
    
    const { data } = await apiClient.post<Repository>(
      "/api/v1/repositories/upload-zip",
      formData,
      {
        headers: { 
          "Content-Type": "multipart/form-data" 
        } 
      }
    );
    return data;
  },
  async getStatus(id: string): Promise<Repository> {
    const { data } = await apiClient.get<Repository>(`/api/v1/repositories/${id}`);
    return data;
  },
  async chat(id: string, message: string): Promise<Record<string, unknown>> {
    const { data } = await apiClient.post<Record<string, unknown>>(
      `/api/v1/repositories/${id}/chat`,
      { question: message },
    );
    return data;
  },

  async getAll(): Promise<Repository[]> {
    const { data } = await apiClient.get<Repository[]>("/api/v1/repositories");
    return data;
  },

  async list(): Promise<Repository[] | null> {
    return this.getAll();
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/api/v1/repositories/${id}`);
  },
};
