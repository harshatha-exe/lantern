package com.harshatha.repo_analyzer.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @AuthenticationPrincipal User user // Automatically extracted from the JWT token!
    ) {
        // 1. Create a new PENDING record tied to the specific user
        RepositoryRecord record = new RepositoryRecord();
        
        // THE FIX: Pass the whole User object instead of just the ID string
        record.setUser(user); 
        
        record.setGithubUrl(request.getGithubUrl());
        record.setStatus("PENDING");
        
        record = repositoryRecordRepository.save(record);

        // 2. Hand off the heavy lifting to the background thread
        analysisOrchestrator.executeAnalysisPipeline(record);

        // 3. Immediately return HTTP 202 (Accepted) to the frontend
        return ResponseEntity.accepted().body(Map.of(
                "id", record.getId(),
                "status", record.getStatus(),
                "message", "Repository queued for analysis"
        ));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getRepositoryStatus(
            @PathVariable java.util.UUID id,
            @AuthenticationPrincipal User user // Security: Know who is asking
    ) {
        return repositoryRecordRepository.findById(id)
                // THE FIX: Navigate through the User object to get the ID for comparison
                .filter(record -> record.getUser().getId().equals(user.getId())) 
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}