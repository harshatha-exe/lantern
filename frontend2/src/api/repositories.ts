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
      { headers: { "Content-Type": "multipart/form-data" } },
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
      { message },
    );
    return data;
  },

  // TODO(backend): No endpoint yet to list all repositories for the current user.
  // Expected: GET /api/v1/repositories  ->  Repository[]
  async list(): Promise<Repository[] | null> {
    return null;
  },

  // TODO(backend): No endpoint yet to delete a repository.
  // Expected: DELETE /api/v1/repositories/{id}
  async remove(_id: string): Promise<void> {
    throw new Error("Delete endpoint not implemented on backend yet.");
  },

  // TODO(backend): No endpoint yet to fetch individual generated artifacts (e.g. README, architecture summary).
  // Expected: GET /api/v1/repositories/{id}/artifacts/{type}
};
