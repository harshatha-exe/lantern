package com.harshatha.repo_analyzer.controller;
import org.springframework.http.MediaType;
import com.harshatha.repo_analyzer.entity.RepositoryRecord;
import com.harshatha.repo_analyzer.entity.User;
import com.harshatha.repo_analyzer.repository.RepositoryRecordRepository;
import com.harshatha.repo_analyzer.service.AnalysisOrchestrator;
import com.harshatha.repo_analyzer.service.ZipIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/repositories") // <-- Updated to match your secured API path
public class RepoUploadController {

    private final ZipIngestionService zipIngestionService;
    private final AnalysisOrchestrator analysisOrchestrator;
    private final RepositoryRecordRepository repositoryRecordRepository;

    public RepoUploadController(ZipIngestionService zipIngestionService, 
                                AnalysisOrchestrator analysisOrchestrator, 
                                RepositoryRecordRepository repositoryRecordRepository) {
        this.zipIngestionService = zipIngestionService;
        this.analysisOrchestrator = analysisOrchestrator;
        this.repositoryRecordRepository = repositoryRecordRepository;
    }

    @PostMapping(value = "/upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadZip(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
                
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".zip")) {
            return ResponseEntity.badRequest().body("Please upload a valid .zip file.");
        }

        try {
            // 1. Create the Database Record
            RepositoryRecord record = new RepositoryRecord();
            record.setGithubUrl("ZIP Upload: " + file.getOriginalFilename());
            record.setUser(user);
            record.setStatus("EXTRACTING");
            record = repositoryRecordRepository.save(record);

            // 2. Extract the Zip (This uses the ZipIngestionService we just wrote)
            java.nio.file.Path extractedPath = zipIngestionService.extractZip(file, record.getId().toString());

            // 3. Fire the massive AI pipeline! (Pass the path to skip cloning)
            analysisOrchestrator.executeAnalysisPipeline(record, extractedPath);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "Zip file uploaded and extraction started.",
                    "repositoryId", record.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process zip: " + e.getMessage());
        }
    }
}