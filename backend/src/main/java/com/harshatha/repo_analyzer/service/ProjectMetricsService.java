package com.harshatha.repo_analyzer.service;

import com.harshatha.repo_analyzer.dto.ProjectMetrics;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class ProjectMetricsService {

    public ProjectMetrics calculateMetrics(Path repoPath) {
        AtomicInteger fileCount = new AtomicInteger(0);
        AtomicLong totalSizeBytes = new AtomicLong(0);

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(p -> {
                        String pathStr = p.toString();
                        // Ignore dependencies and hidden Git history to get the TRUE project size
                        return !pathStr.contains(".git") 
                                && !pathStr.contains("node_modules") 
                                && !pathStr.contains("target") 
                                && !pathStr.contains("build");
                    })
                 .filter(Files::isRegularFile)
                 .forEach(file -> {
                     fileCount.incrementAndGet();
                     try {
                         totalSizeBytes.addAndGet(Files.size(file));
                     } catch (Exception e) {
                         // Silently ignore files that can't be read
                     }
                 });
        } catch (Exception e) {
            System.err.println("Failed to calculate project metrics: " + e.getMessage());
        }

        return new ProjectMetrics(fileCount.get(), totalSizeBytes.get() / 1024); // Convert Bytes to KB
    }
}