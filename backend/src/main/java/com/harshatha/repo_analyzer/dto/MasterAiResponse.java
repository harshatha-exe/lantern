package com.harshatha.repo_analyzer.dto;

public record MasterAiResponse(
        String summary,
        String annotatedTree,
        String healthCheck,
        String readme,
        String resumeBullets,
        String interviewQuestions
) {}