package com.harshatha.repo_analyzer.service;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harshatha.repo_analyzer.repository.RepoChunk;
import com.harshatha.repo_analyzer.repository.RepoChunkRepository;

import java.util.*;

@Service
public class RepoRagService {

    @Value("${jina.api.key}")
    private String jinaApiKey;

    private final RepoChunkRepository chunkRepository;
    private final RestClient restClient = RestClient.builder().build(); // Properly initialized
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RepoRagService(RepoChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public void chunkAndEmbedRepository(UUID repoId, Path repoPath) {
        List<RepoChunk> pendingChunks = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String str = p.toString().toLowerCase();
                     return !str.contains(".git") && !str.contains("node_modules") 
                             && !str.contains("target") && !str.contains("venv");
                 })
                 .forEach(file -> {
                     try {
                         String content = Files.readString(file);
                         String relativePath = repoPath.relativize(file).toString();
                         
                         List<String> chunks = splitIntoChunks(content, 700, 150);
                         for (String chunkText : chunks) {
                             RepoChunk rc = new RepoChunk();
                             rc.setRepositoryId(repoId);
                             rc.setFilePath(relativePath);
                             rc.setContent(chunkText);
                             pendingChunks.add(rc);
                         }
                     } catch (Exception ignored) {}
                 });

            System.out.println("Total chunks generated: " + pendingChunks.size() + ". Sending to Hugging Face in batches...");
            processBatches(pendingChunks, repoId);

        } catch (Exception e) {
            System.err.println("RAG Ingestion failed: " + e.getMessage());
        }
    }

    private void processBatches(List<RepoChunk> allChunks, UUID repoId) {
        int batchSize = 5; 
        
        for (int i = 0; i < allChunks.size(); i += batchSize) {
            List<RepoChunk> batch = allChunks.subList(i, Math.min(i + batchSize, allChunks.size()));
            List<String> textsToEmbed = batch.stream().map(RepoChunk::getContent).toList();
            
            try {
                List<String> vectors = getBatchEmbeddingsWithRetry(textsToEmbed);
                
                for (int j = 0; j < batch.size(); j++) {
                    if (j < vectors.size()) {
                        chunkRepository.insertChunkWithVector(
                            repoId,
                            batch.get(j).getFilePath(),
                            batch.get(j).getContent(),
                            vectors.get(j)
                        );
                    }
                }
                Thread.sleep(1000); // 1-second cooldown between batches
            } catch (Exception e) {
                System.err.println("Failed processing batch " + (i/batchSize + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Handles the "Cold Start" model loading issue
    private List<String> getBatchEmbeddingsWithRetry(List<String> texts) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return executeJinaApiCall(texts);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("currently loading") || e.getMessage().contains("503")) {
                    System.out.println("Hugging Face model is waking up (Attempt " + attempt + "/3). Waiting 15 seconds...");
                    Thread.sleep(15000);
                } else {
                    throw e; // Throw if it's a real error (like a bad API key)
                }
            }
        }
        throw new RuntimeException("Hugging Face model failed to load after 3 attempts.");
    }

    private List<String> executeJinaApiCall(List<String> texts) throws Exception {
        Map<String, Object> body = Map.of(
            "model", "jina-embeddings-v3",
            "input", texts
        );

        String responseString = restClient.post()
                .uri("https://api.jina.ai/v1/embeddings")
                .header("Authorization", "Bearer " + jinaApiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseString);
        List<String> embeddingsList = new ArrayList<>();
        
        // Jina returns a clean "data" array
        for (JsonNode dataNode : root.get("data")) {
            embeddingsList.add(dataNode.get("embedding").toString());
        }
        return embeddingsList;
    }

    // Also update the single embedding fetch used during Chat
    public String getSingleEmbeddingFromHuggingFace(String text) {
        try {
            List<String> result = getBatchEmbeddingsWithRetry(List.of(text));
            if (!result.isEmpty()) return result.get(0);
        } catch (Exception e) {
            System.err.println("Chat Embedding failed: " + e.getMessage());
        }
        return Arrays.toString(new float[1024]);
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
}