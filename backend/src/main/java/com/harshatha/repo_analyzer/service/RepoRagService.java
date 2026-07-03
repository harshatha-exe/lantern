package com.harshatha.repo_analyzer.service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harshatha.repo_analyzer.repository.RepoChunk;
import com.harshatha.repo_analyzer.repository.RepoChunkRepository;

@Service
public class RepoRagService {

    @Value("${jina.api.key}")
    private String jinaApiKey;    
    private final TransactionTemplate transactionTemplate; 
    private final RepoChunkRepository chunkRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient = RestClient.builder().build(); 
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<RepoChunk> globalChunkBuffer = new ArrayList<>();
    private final int BATCH_SIZE = 50;

    public RepoRagService(RepoChunkRepository chunkRepository, TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
        this.chunkRepository = chunkRepository;
        this.transactionTemplate = transactionTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }


    public void chunkAndEmbedRepository(UUID repoId, Path repoPath) {
        System.out.println("Starting global-buffered RAG ingestion for repo: " + repoId);

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isNotExcluded)
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
                             globalChunkBuffer.add(rc);
                             
                             if (globalChunkBuffer.size() >= BATCH_SIZE) {
                                 flushBuffer();
                             }
                         }
                     } catch (Exception e) {
                         System.err.println("Failed to read/process file: " + file.toString());
                     }
                 });
                 
            if (!globalChunkBuffer.isEmpty()) {
                flushBuffer();
            }

            System.out.println("Finished processing all files for repo: " + repoId);

        } catch (Exception e) {
            System.err.println("RAG Ingestion encountered a fatal error:");
            e.printStackTrace();
        }
    }

    private void flushBuffer() {
        if (globalChunkBuffer.isEmpty()) return;

        System.out.println("Flushing batch of " + globalChunkBuffer.size() + " chunks...");

        List<String> textsToEmbed = globalChunkBuffer.stream().map(RepoChunk::getContent).toList();
        
        try {
            long apiStart = System.currentTimeMillis();
            List<String> vectors = getBatchEmbeddingsWithRetry(textsToEmbed);
            System.out.println("  -> \u23F1 Jina API took " + (System.currentTimeMillis() - apiStart) + "ms");
            
            long dbStart = System.currentTimeMillis();
            
            String sql = "INSERT INTO repo_chunks (repository_id, file_path, content, embedding) VALUES (?, ?, ?, ?::vector)";
            
            List<Object[]> batchArgs = new ArrayList<>();
            for (int j = 0; j < globalChunkBuffer.size(); j++) {
                if (j < vectors.size()) {
                    batchArgs.add(new Object[]{
                            globalChunkBuffer.get(j).getRepositoryId(),
                            globalChunkBuffer.get(j).getFilePath(),
                            globalChunkBuffer.get(j).getContent(),
                            vectors.get(j) 
                    });
                }
            }
            
            jdbcTemplate.batchUpdate(sql, batchArgs);
            
            System.out.println("  -> \u23F1 Database Inserts took " + (System.currentTimeMillis() - dbStart) + "ms");

        } catch (Exception e) {
            System.err.println("Failed to process a global chunk batch.");
            e.printStackTrace();
        }
        globalChunkBuffer.clear();
        try {
            Thread.sleep(2000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isNotExcluded(Path p) {
        String str = p.toString().toLowerCase();
        return !str.contains(".git") && 
               !str.contains("node_modules") && 
               !str.contains("target") && 
               !str.contains("venv");
    }

    private void processIndividualFileChunks(UUID repoId, String filePath, List<String> chunks) {
        int batchSize = 50;
        
        for (int i = 0; i < chunks.size(); i += batchSize) {
            List<String> subBatch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
            
            try {
                List<String> vectors = getBatchEmbeddingsWithRetry(subBatch);
                
                for (int j = 0; j < subBatch.size(); j++) {
                    if (j < vectors.size()) {
                        chunkRepository.insertChunkWithVector(
                            repoId,
                            filePath,
                            subBatch.get(j),
                            vectors.get(j)
                        );
                    }
                }
                
                //Thread.sleep(500); 
                
            } catch (Exception e) {
                System.err.println("Failed to process a chunk batch for file: " + filePath);
                e.printStackTrace();
            }
        }
    }

    private List<String> getBatchEmbeddingsWithRetry(List<String> texts) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return executeJinaApiCall(texts);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("currently loading") || e.getMessage().contains("503") || e.getMessage().contains("429")) {
                    System.out.println("API limit/loading (Attempt " + attempt + "/3). Waiting 5 seconds...");
                    Thread.sleep(5000);
                } else {
                    throw e; 
                }
            }
        }
        throw new RuntimeException("API failed to process request after 3 attempts.");
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
        
        for (JsonNode dataNode : root.get("data")) {
            embeddingsList.add(dataNode.get("embedding").toString());
        }
        return embeddingsList;
    }

    // Chat logic
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