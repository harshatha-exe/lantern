package com.harshatha.repo_analyzer.service;

import java.nio.file.Path;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.harshatha.repo_analyzer.dto.TechStackReport;
import com.harshatha.repo_analyzer.entity.RepositoryRecord; // IMPORT ADDED
import com.harshatha.repo_analyzer.repository.RepositoryRecordRepository;

@Service
public class AnalysisOrchestrator {

    private final RepositoryRecordRepository repositoryRecordRepository;
    private final GitIngestionService gitIngestionService;
    private final TechStackDetectionService techStackDetectionService;
    private final AiSummaryService aiSummaryService; 
    private final ProjectMetricsService projectMetricsService;

    public AnalysisOrchestrator(RepositoryRecordRepository repositoryRecordRepository, 
                                GitIngestionService gitIngestionService,
                                TechStackDetectionService techStackDetectionService,
                                AiSummaryService aiSummaryService,
                                ProjectMetricsService projectMetricsService) {
        this.repositoryRecordRepository = repositoryRecordRepository;
        this.gitIngestionService = gitIngestionService;
        this.techStackDetectionService = techStackDetectionService;
        this.aiSummaryService = aiSummaryService;
        this.projectMetricsService = projectMetricsService;
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
            
            System.out.println("--- DETECTED TECH STACK ---");
            System.out.println("Languages: " + detectedStack.languages());
            System.out.println("Frameworks: " + detectedStack.frameworks());
            System.out.println("Databases: " + detectedStack.databases());
            System.out.println("---------------------------");

        
            System.out.println("Calculating project complexity metrics...");
            com.harshatha.repo_analyzer.dto.ProjectMetrics metrics = projectMetricsService.calculateMetrics(repoPath);
            System.out.println("Total Files: " + metrics.totalFiles() + " | Size: " + metrics.totalSizeKb() + " KB");

            // 3. AI SUMMARY GENERATION
            System.out.println("Sending data to Gemini AI for summary generation...");
            String aiSummary = aiSummaryService.generateRepositorySummary(repoPath, detectedStack);
            
            System.out.println("--- AI SUMMARY ---");
            System.out.println(aiSummary);
            System.out.println("------------------");

            // 4. MAP STRUCTURE & ARCHITECTURE
            System.out.println("Mapping project structure...");
            String projectTree = aiSummaryService.generateProjectTree(repoPath);
            
            System.out.println("Sending data to Gemini AI for architecture & health analysis...");
            com.harshatha.repo_analyzer.dto.StructureAnalysisResult structureResult = 
                    aiSummaryService.analyzeStructureAndHealth(projectTree, detectedStack);
            
            // ---> NEW: README GENERATION <---
            System.out.println("Drafting comprehensive README.md...");
            String generatedReadme = aiSummaryService.generateReadme(
                    aiSummary, 
                    detectedStack, 
                    structureResult.healthCheck(), 
                    structureResult.annotatedTree()
            );             

            // ---> ADD A PAUSE TO AVOID GOOGLE 503 / 429 ERRORS <---
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

            // ---> NEW: JOB HUNTER MODULE <---
            System.out.println("Generating Resume Bullets & Interview Questions...");
            com.harshatha.repo_analyzer.dto.JobHunterResult jobHunterData = 
                    aiSummaryService.generateJobHunterAssets(aiSummary, detectedStack);


            // 5. PACK DATA FOR NORMALIZED DB
            com.harshatha.repo_analyzer.entity.Analysis analysis = new com.harshatha.repo_analyzer.entity.Analysis();
            analysis.setTechStack(detectedStack.toString());
            analysis.setSummary(aiSummary);
            analysis.setProjectStructure(structureResult.annotatedTree());
            analysis.setArchitecturePattern(structureResult.healthCheck());
            analysis.setGeneratedReadme(generatedReadme);
            
            // ---> Save the metrics to the DB <---
            analysis.setTotalFiles(metrics.totalFiles());
            analysis.setTotalSizeKb(metrics.totalSizeKb());
            analysis.setResumeBullets(jobHunterData.resumeBullets());
            analysis.setInterviewQuestions(jobHunterData.interviewQuestions());
            
            analysis.setRepositoryRecord(record); 
            record.setAnalysis(analysis);

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