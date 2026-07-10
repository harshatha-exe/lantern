package com.harshatha.repo_analyzer.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.harshatha.repo_analyzer.dto.AnalyzeRepoRequest;
import com.harshatha.repo_analyzer.entity.RepositoryRecord;
import com.harshatha.repo_analyzer.entity.User;
import com.harshatha.repo_analyzer.repository.AnalysisRepository;
import com.harshatha.repo_analyzer.repository.RepoChunkRepository;
import com.harshatha.repo_analyzer.repository.RepositoryRecordRepository;
import com.harshatha.repo_analyzer.service.AnalysisOrchestrator;
import com.harshatha.repo_analyzer.service.GitHubValidationService;
import com.harshatha.repo_analyzer.service.ZipIngestionService;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryRecordRepository repositoryRecordRepository;
    private final AnalysisRepository analysisRepository;
    private final RepoChunkRepository repoChunkRepository;
    private final AnalysisOrchestrator analysisOrchestrator;
    private final GitHubValidationService gitHubValidationService; 
    private final ZipIngestionService zipIngestionService; 

    public RepositoryController(
            RepositoryRecordRepository repositoryRecordRepository,
            AnalysisRepository analysisRepository,
            RepoChunkRepository repoChunkRepository,
            AnalysisOrchestrator analysisOrchestrator,
            GitHubValidationService gitHubValidationService,
            ZipIngestionService zipIngestionService 
    ) {
        this.repositoryRecordRepository = repositoryRecordRepository;
        this.analysisRepository = analysisRepository;
        this.repoChunkRepository = repoChunkRepository;
        this.analysisOrchestrator = analysisOrchestrator;
        this.gitHubValidationService = gitHubValidationService;
        this.zipIngestionService = zipIngestionService;
    }

    // 1. GitHub URL Upload
    @PostMapping
    public ResponseEntity<?> submitRepository(
            @RequestBody AnalyzeRepoRequest request,
            @AuthenticationPrincipal User user
    ) {
        if (request.getGithubUrl() != null && request.getGithubUrl().contains("github.com")) {
            gitHubValidationService.validateRepositorySize(request.getGithubUrl());
        }

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

    // 2. ZIP File Upload 
    @PostMapping(value = "/upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadZip(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".zip")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid .zip file."));
        }

        if (file.getSize() > 500 * 1024) { 
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                    .body(Map.of("error", "ZIP file exceeds the 500KB limit for the free tier."));
        }

        try {
            RepositoryRecord record = new RepositoryRecord();
            record.setGithubUrl("ZIP Upload: " + file.getOriginalFilename());
            record.setUser(user);
            record.setStatus("EXTRACTING");
            record = repositoryRecordRepository.save(record);

            java.nio.file.Path extractedPath = zipIngestionService.extractZip(file, record.getId().toString());

            analysisOrchestrator.executeAnalysisPipeline(record, extractedPath);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "Zip file uploaded and extraction started.",
                    "repositoryId", record.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process zip: " + e.getMessage()));
        }
    }
    
    // 3. Get Single Repo Status
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

    // 4. Get All Repos for User
    @GetMapping
    public ResponseEntity<List<RepositoryRecord>> getUserRepositories(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(repositoryRecordRepository.findAllByUserId(user.getId()));
    }

    // 5. Delete Repo
    @DeleteMapping("/{id}")
    @Transactional 
    public ResponseEntity<?> deleteRepository(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return repositoryRecordRepository.findById(id)
                .filter(record -> record.getUser().getId().equals(user.getId()))
                .map(record -> {
                    repoChunkRepository.deleteByRepositoryId(record.getId());
                    analysisRepository.deleteByRepositoryRecordId(record.getId());
                    repositoryRecordRepository.delete(record);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}