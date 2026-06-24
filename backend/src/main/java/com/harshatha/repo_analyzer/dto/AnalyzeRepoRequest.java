package com.harshatha.repo_analyzer.dto;

public class AnalyzeRepoRequest {
    private String githubUrl = "";

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }
}