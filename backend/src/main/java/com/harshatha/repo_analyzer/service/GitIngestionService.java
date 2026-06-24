package com.harshatha.repo_analyzer.service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

@Service
public class GitIngestionService {

    public Path cloneRepository(String githubUrl, String repoId) {
        try {
            Path tempDir = Files.createTempDirectory("repo-analyzer-" + repoId + "-");
            File directory = tempDir.toFile();

            System.out.println("Starting clone for " + githubUrl + " into " + directory.getAbsolutePath());

            // THE FIX: Wrap the JGit call in a try-with-resources block
            // This guarantees JGit releases the Windows file lock instantly after cloning!
            try (Git git = Git.cloneRepository()
                    .setURI(githubUrl)
                    .setDirectory(directory)
                    .setDepth(1) 
                    .call()) {
                
                System.out.println("Successfully cloned: " + githubUrl);
            }
            
            return tempDir;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone repository: " + githubUrl, e);
        }
    }
}
