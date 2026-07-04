package com.harshatha.repo_analyzer.service;

import java.io.IOException;
import java.nio.file.Files; 
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ZipIngestionService {
    private static final long MAX_UNCOMPRESSED_SIZE = 2 * 1024 * 1024;

    public Path extractZip(MultipartFile file, String repoId) throws IOException {
        Path tempDir = Files.createTempDirectory("repo-analyzer-zip-" + repoId + "-");
        System.out.println("Extracting ZIP to: " + tempDir.toAbsolutePath());

        long totalExtractedBytes = 0; 

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolvedPath = tempDir.resolve(entry.getName()).normalize();

                if (!resolvedPath.startsWith(tempDir)) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    
                    try (var outputStream = Files.newOutputStream(resolvedPath)) {
                        byte[] buffer = new byte[8192]; 
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            
                            totalExtractedBytes += len;
                            
                            if (totalExtractedBytes > MAX_UNCOMPRESSED_SIZE) {
                                throw new IOException("Extraction aborted: Uncompressed size exceeds the 2MB free tier limit.");
                            }
                            
                            outputStream.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            System.err.println("Zip extraction failed, cleaning up disk: " + e.getMessage());
            FileSystemUtils.deleteRecursively(tempDir);
            throw new IOException("Failed to extract zip: " + e.getMessage(), e); 
        }
        return tempDir;
    }
}