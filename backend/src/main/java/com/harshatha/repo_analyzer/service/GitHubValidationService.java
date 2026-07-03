package com.harshatha.repo_analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GitHubValidationService {

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validateRepositorySize(String githubUrl) {
        try {
            // 1. Extract owner and repo from URL (e.g., https://github.com/facebook/react)
            String cleanUrl = githubUrl.replace("https://github.com/", "").replace(".git", "");
            String[] parts = cleanUrl.split("/");
            
            if (parts.length < 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GitHub URL format.");
            }

            String owner = parts[0];
            String repo = parts[1];

            // 2. Call GitHub API (No auth required for public repos)
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo;
            
            String responseString = restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseString);
            
            // 3. GitHub API returns size in Kilobytes (KB)
            int sizeInKb = root.get("size").asInt();

            System.out.println("Validating Repo: " + owner + "/" + repo + " | Size: " + sizeInKb + "KB");

            // 4. Enforce the 500KB Limit
            if (sizeInKb > 500) {
                throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, 
                    "Repository is too large (" + sizeInKb + "KB). Maximum allowed size is 500KB for the free tier."
                );
            }

        } catch (ResponseStatusException e) {
            throw e; // Re-throw our custom exception
        } catch (Exception e) {
            System.err.println("Failed to fetch GitHub repo size: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not validate repository size. Make sure it is public.");
        }
    }
}