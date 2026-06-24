package com.harshatha.repo_analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipIngestionService {

    public Path extractZip(MultipartFile file, String repoId) throws IOException {
        // Create the temporary directory
        Path tempDir = Files.createTempDirectory("repo-analyzer-zip-" + repoId + "-");
        System.out.println("Extracting ZIP to: " + tempDir.toAbsolutePath());

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolvedPath = tempDir.resolve(entry.getName()).normalize();

                // Security check: Prevent "Zip Slip" vulnerability
                if (!resolvedPath.startsWith(tempDir)) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
        return tempDir;
    }
}