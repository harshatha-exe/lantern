package com.harshatha.repo_analyzer.service;

import com.harshatha.repo_analyzer.repository.RepoChunk;
import com.harshatha.repo_analyzer.repository.RepoChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
public class RepoRagService {

    private final RepoChunkRepository chunkRepository;
    private final RestClient restClient = RestClient.create();

    public RepoRagService(RepoChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public void chunkAndEmbedRepository(UUID repoId, Path repoPath) {
        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String str = p.toString().toLowerCase();
                     return !str.contains(".git") && !str.contains("node_modules") && !str.contains("target");
                 })
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         String relativePath = repoPath.relativize(file).toString();
                         
                         List<String> chunks = splitIntoChunks(content, 700, 150);
                         for (String chunk : chunks) {
                             String embedding = getLocalEmbedding(chunk);
                             
                             RepoChunk rc = new RepoChunk();
                             rc.setRepositoryId(repoId);
                             rc.setFilePath(relativePath);
                             rc.setContent(chunk);
                             rc.setEmbedding(embedding);
                             
                             chunkRepository.save(rc);
                         }
                     } catch (Exception ignored) {}
                 });
        } catch (Exception e) {
            System.err.println("RAG Ingestion failed: " + e.getMessage());
        }
    }

    private List<String> splitIntoChunks(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            chunks.add(text.substring(start, end));
            start += (size - overlap);
        }
        return chunks;
    }

    public String getLocalEmbedding(String text) {
        try {
            Map<String, Object> body = Map.of("inputs", text);
            List<?> response = restClient.post()
                    .uri("https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2")
                    .body(body)
                    .retrieve()
                    .body(List.class);
            return Objects.requireNonNull(response).toString();
        } catch (Exception e) {
            float[] fallback = new float[384];
            return Arrays.toString(fallback);
        }
    }
}