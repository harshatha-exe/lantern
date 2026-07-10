package com.harshatha.repo_analyzer.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.harshatha.repo_analyzer.entity.RepositoryRecord;

public interface RepositoryRecordRepository extends JpaRepository<RepositoryRecord, UUID> {
    List<RepositoryRecord> findAllByUserId(UUID userId);
}
