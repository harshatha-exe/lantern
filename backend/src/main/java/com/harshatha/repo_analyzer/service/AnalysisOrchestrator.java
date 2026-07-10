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

    // Helper method to extract the clean name from either a ZIP upload or a GitHub URL
    private String extractCleanRepoName(String originalUrl) {
        if (originalUrl == null) return "Project";

        if (originalUrl.startsWith("ZIP Upload: ")) {
            String filename = originalUrl.replace("ZIP Upload: ", "").trim();
            if (filename.toLowerCase().endsWith(".zip")) {
                filename = filename.substring(0, filename.length() - 4);
            }
            return filename;
        } else if (originalUrl.contains("github.com")) {
            String cleanUrl = originalUrl.replace(".git", "");
            String[] parts = cleanUrl.split("/");
            return parts[parts.length - 1]; 
        }
        
        return "Project";
    }

    @Async
    public void executeAnalysisPipeline(RepositoryRecord record) {
        executeAnalysisPipeline(record, null);
    }

    @Async
    public void executeAnalysisPipeline(RepositoryRecord record, Path preExtractedPath) {
        Path repoPath = null; 
        
        try {
            // 1. INGESTION
            if (preExtractedPath == null) {
                record.setStatus("CLONING");
                repositoryRecordRepository.save(record);
                repoPath = gitIngestionService.cloneRepository(
                        record.getGithubUrl(), 
                        record.getId().toString()
                );
            } else {
                repoPath = preExtractedPath; 
            }
            
            String cleanRepoName = extractCleanRepoName(record.getGithubUrl());
            
            // 2. ANALYZING (Tech Stack)
            record.setStatus("ANALYZING");
            repositoryRecordRepository.save(record);
            TechStackReport detectedStack = techStackDetectionService.detectStack(repoPath);

            System.out.println("Calculating project complexity metrics...");
            com.harshatha.repo_analyzer.dto.ProjectMetrics metrics = projectMetricsService.calculateMetrics(repoPath);

            System.out.println("Generating Project Tree for: " + cleanRepoName);
            String projectTree = aiSummaryService.generateProjectTree(repoPath, cleanRepoName);

            System.out.println("Sending data to Gemini AI for Master Analysis (1 API Call)...");
            com.harshatha.repo_analyzer.dto.MasterAiResponse masterResponse = 
                    aiSummaryService.generateMasterAnalysis(detectedStack, projectTree, cleanRepoName);

            // 5. PACK DATA FOR NORMALIZED DB
            com.harshatha.repo_analyzer.entity.Analysis analysis = new com.harshatha.repo_analyzer.entity.Analysis();
            analysis.setTechStack(detectedStack.toString());
            analysis.setTotalFiles(metrics.totalFiles());
            analysis.setTotalSizeKb(metrics.totalSizeKb());
            
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
            if (record != null && !"COMPLETED".equals(record.getStatus())) {
                record.setStatus("FAILED");
                repositoryRecordRepository.save(record);
            }
        } finally {
            if (repoPath != null) {
                try {
                    System.out.println("Attempting post-analysis file cleanup for: " + repoPath);
                    org.springframework.util.FileSystemUtils.deleteRecursively(repoPath);
                    System.out.println("Cleaned up temporary directory successfully.");
                } catch (Exception cleanupEx) {
                    System.err.println("Warning: Local cleanup skipped due to Windows file lock. " +
                            "This is safe to ignore; the host OS will automatically purge this temp folder. " +
                            "Details: " + cleanupEx.getMessage());
                }
            }
        }
    }
}