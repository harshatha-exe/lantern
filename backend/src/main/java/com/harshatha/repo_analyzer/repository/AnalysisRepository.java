package com.harshatha.repo_analyzer.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.harshatha.repo_analyzer.entity.Analysis;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
    void deleteByRepositoryRecordId(UUID repositoryRecordId);
}