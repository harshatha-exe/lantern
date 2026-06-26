package com.harshatha.repo_analyzer.service;

import java.nio.file.Path;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.harshatha.repo_analyzer.dto.TechStackReport;
import com.harshatha.repo_analyzer.entity.RepositoryRecord; 
import com.harshatha.repo_analyzer.repository.RepositoryRecordRepository;

@Service
public class AnalysisOrchestrator {
    
    private final RepositoryRecordRepository repositoryRecordRepository;
    private final GitIngestionService gitIngestionService;
    private final TechStackDetectionService techStackDetectionService;
    private final AiSummaryService aiSummaryService; 
    private final ProjectMetricsService projectMetricsService;

    private final RepoRagService repoRagService; 

public AnalysisOrchestrator(
        GitIngestionService gitIngestionService,
        TechStackDetectionService techStackDetectionService,
        ProjectMetricsService projectMetricsService,
        AiSummaryService aiSummaryService,
        RepositoryRecordRepository repositoryRecordRepository,
        RepoRagService repoRagService) { 
    this.gitIngestionService = gitIngestionService;
    this.techStackDetectionService = techStackDetectionService;
    this.projectMetricsService = projectMetricsService;
    this.aiSummaryService = aiSummaryService;
    this.repositoryRecordRepository = repositoryRecordRepository;
    this.repoRagService = repoRagService; 
}

    @Async
    public void executeAnalysisPipeline(RepositoryRecord record) {
        // If called normally via GitHub, pass null for the zip path
        executeAnalysisPipeline(record, null);
    }

    @Async
    public void executeAnalysisPipeline(RepositoryRecord record, Path preExtractedPath) {
        Path repoPath = null; // Declare outside so the finally block can see it
        
        try {
            // 1. INGESTION (Clone OR Zip)
            if (preExtractedPath == null) {
                record.setStatus("CLONING");
                repositoryRecordRepository.save(record);
                repoPath = gitIngestionService.cloneRepository(
                        record.getGithubUrl(), 
                        record.getId().toString()
                );
            } else {
                repoPath = preExtractedPath; // Bypass cloning! Use the ZIP folder!
            }

            // 2. ANALYZING (Tech Stack)
            record.setStatus("ANALYZING");
            repositoryRecordRepository.save(record);
            TechStackReport detectedStack = techStackDetectionService.detectStack(repoPath);

            System.out.println("Calculating project complexity metrics...");
            com.harshatha.repo_analyzer.dto.ProjectMetrics metrics = projectMetricsService.calculateMetrics(repoPath);

            // ---> THE NEW, SINGLE AI CALL <---
            System.out.println("Generating Project Tree...");
            String projectTree = aiSummaryService.generateProjectTree(repoPath);

            System.out.println("Sending data to Gemini AI for Master Analysis (1 API Call)...");
            com.harshatha.repo_analyzer.dto.MasterAiResponse masterResponse = 
                    aiSummaryService.generateMasterAnalysis(detectedStack, projectTree);

            // 5. PACK DATA FOR NORMALIZED DB
            com.harshatha.repo_analyzer.entity.Analysis analysis = new com.harshatha.repo_analyzer.entity.Analysis();
            analysis.setTechStack(detectedStack.toString());
            analysis.setTotalFiles(metrics.totalFiles());
            analysis.setTotalSizeKb(metrics.totalSizeKb());
            
            // Map the unified AI response!
            analysis.setSummary(masterResponse.summary());
            analysis.setProjectStructure(masterResponse.annotatedTree());
            analysis.setArchitecturePattern(masterResponse.healthCheck());
            analysis.setGeneratedReadme(masterResponse.readme());
            analysis.setResumeBullets(masterResponse.resumeBullets());
            analysis.setInterviewQuestions(masterResponse.interviewQuestions());
            
            analysis.setRepositoryRecord(record); 
            record.setAnalysis(analysis); 

            System.out.println("Building code index mapping inside vector store...");
            repoRagService.chunkAndEmbedRepository(record.getId(), repoPath);
            
            // 6. MARK AS COMPLETED
            record.setStatus("COMPLETED");
            repositoryRecordRepository.save(record);

        } catch (Exception e) {
            System.err.println("Analysis pipeline failed: " + e.getMessage());
            // Only mark as FAILED if it didn't manage to hit COMPLETED
            if (record != null && !"COMPLETED".equals(record.getStatus())) {
                record.setStatus("FAILED");
                repositoryRecordRepository.save(record);
            }
        } finally {
            // THE ARCHITECTURAL FIX: Isolated cleanup block
            if (repoPath != null) {
                try {
                    System.out.println("Attempting post-analysis file cleanup for: " + repoPath);
                    org.springframework.util.FileSystemUtils.deleteRecursively(repoPath);
                    System.out.println("Cleaned up temporary directory successfully.");
                } catch (Exception cleanupEx) {
                    // Log as a minor warning, do NOT throw an exception or alter the DB status
                    System.err.println("Warning: Local cleanup skipped due to Windows file lock. " +
                            "This is safe to ignore; the host OS will automatically purge this temp folder. " +
                            "Details: " + cleanupEx.getMessage());
                }
            }
        }
    }
}