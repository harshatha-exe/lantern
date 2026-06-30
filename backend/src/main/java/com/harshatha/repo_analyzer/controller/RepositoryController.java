package com.harshatha.repo_analyzer.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.harshatha.repo_analyzer.dto.AnalyzeRepoRequest;
import com.harshatha.repo_analyzer.entity.RepositoryRecord;
import com.harshatha.repo_analyzer.entity.User;
import com.harshatha.repo_analyzer.repository.RepositoryRecordRepository;
import com.harshatha.repo_analyzer.service.AnalysisOrchestrator;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryRecordRepository repositoryRecordRepository;
    private final AnalysisOrchestrator analysisOrchestrator;

    public RepositoryController(RepositoryRecordRepository repositoryRecordRepository, 
                                AnalysisOrchestrator analysisOrchestrator) {
        this.repositoryRecordRepository = repositoryRecordRepository;
        this.analysisOrchestrator = analysisOrchestrator;
    }

    @PostMapping
    public ResponseEntity<?> submitRepository(
            @RequestBody AnalyzeRepoRequest request,
            @AuthenticationPrincipal User user
    ) {
        RepositoryRecord record = new RepositoryRecord();
        record.setUser(user); 
        record.setGithubUrl(request.getGithubUrl());
        record.setStatus("PENDING");
        record = repositoryRecordRepository.save(record);

        analysisOrchestrator.executeAnalysisPipeline(record);

        return ResponseEntity.accepted().body(Map.of(
                "id", record.getId(),
                "status", record.getStatus(),
                "message", "Repository queued for analysis"
        ));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getRepositoryStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return repositoryRecordRepository.findById(id)
                .filter(record -> record.getUser().getId().equals(user.getId())) 
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RepositoryRecord>> getUserRepositories(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(repositoryRecordRepository.findAllByUserId(user.getId()));
    }
}