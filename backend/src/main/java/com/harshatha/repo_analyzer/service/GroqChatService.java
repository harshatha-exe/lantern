package com.harshatha.repo_analyzer.service;

import com.harshatha.repo_analyzer.repository.RepoChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class GroqChatService {

    @Value("${groq.api.url}") private String groqUrl;
    @Value("${groq.api.key}") private String groqApiKey;

    private final RepoChunkRepository chunkRepository;
    private final RepoRagService ragService;
    private final RestClient restClient = RestClient.create();

    public GroqChatService(RepoChunkRepository chunkRepository, RepoRagService ragService) {
        this.chunkRepository = chunkRepository;
        this.ragService = ragService;
    }

    public String askCodebase(UUID repoId, String userQuestion) {
        String queryVector = ragService.getLocalEmbedding(userQuestion);

        List<RepoChunkRepository.ChunkSearchResult> matches = 
                chunkRepository.searchSimilarChunks(repoId, queryVector, 0.3, 4);

        StringBuilder contextBuilder = new StringBuilder();
        for (var match : matches) {
            contextBuilder.append("\n--- File: ").append(match.getFilePath()).append(" ---\n")
                          .append(match.getContent()).append("\n");
        }

        String systemInstructions = """
            You are an expert AI assistant with direct access to a codebase. 
            Answer the user's question using ONLY the following verified source snippets. 
            If you do not know the answer, state honestly that you can't find it in the provided files.
            
            Codebase Context:
            """ + contextBuilder.toString();

        Map<String, Object> requestBody = Map.of(
            "model", "llama-3.1-8b-instant", 
            "messages", List.of(
                Map.of("role", "system", "content", systemInstructions),
                Map.of("role", "user", "content", userQuestion)
            ),
            "temperature", 0.2
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri(groqUrl)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.of().getClass());

            List<?> choices = (List<?>) Objects.requireNonNull(response).get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            return message.get("content").toString();
            
        } catch (Exception e) {
            return "Groq Engine unavailable: " + e.getMessage();
        }
    }
}