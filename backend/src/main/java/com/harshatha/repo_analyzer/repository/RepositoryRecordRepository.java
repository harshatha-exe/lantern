package com.harshatha.repo_analyzer.repository;

import com.harshatha.repo_analyzer.entity.RepositoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RepositoryRecordRepository extends JpaRepository<RepositoryRecord, UUID> {
    // Allows us to fetch all repositories submitted by a specific user
    List<RepositoryRecord> findAllByUserId(UUID userId);
}