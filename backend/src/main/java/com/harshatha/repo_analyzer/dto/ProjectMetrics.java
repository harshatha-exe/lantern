package com.harshatha.repo_analyzer.dto;

public record ProjectMetrics(
        int totalFiles,
        long totalSizeKb
) {}