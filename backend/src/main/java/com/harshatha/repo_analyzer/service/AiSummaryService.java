package com.harshatha.repo_analyzer.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.harshatha.repo_analyzer.dto.MasterAiResponse;
import com.harshatha.repo_analyzer.dto.TechStackReport;

@Service
public class AiSummaryService {

    private final RestClient restClient;

    @Value("${application.ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${application.ai.gemini.url}")
    private String geminiUrl;

    public AiSummaryService() {
        this.restClient = RestClient.create();
    }

    public String generateProjectTree(Path repoPath, String cleanRepoName) {
        StringBuilder tree = new StringBuilder();
        try (Stream<Path> paths = Files.walk(repoPath, 5)) { 
            paths.filter(p -> {
                String pathStr = p.toString();
                return !pathStr.contains(".git") && !pathStr.contains("node_modules") 
                    && !pathStr.contains("target") && !pathStr.contains("build")
                    && !pathStr.contains(".idea") && !pathStr.contains("dist");
            })
            .limit(100) 
            .forEach(p -> {
                if (p.equals(repoPath)) {
                    tree.append("📁 ").append(cleanRepoName).append("\n");
                } else {
                    int depth = repoPath.relativize(p).getNameCount();
                    String indent = "  ".repeat(depth); 
                    String marker = Files.isDirectory(p) ? "📁 " : "📄 ";
                    tree.append(indent).append(marker).append(p.getFileName()).append("\n");
                }
            });
        } catch (Exception e) {
            return "Could not map directory structure.";
        }
        return tree.toString();
    }

    public MasterAiResponse generateMasterAnalysis(TechStackReport techStack, String projectTree, String cleanRepoName) {
        String prompt = """
            You are a Senior Software Architect and Expert Technical Recruiter analyzing a codebase.
            
            The name of this repository is '%s'.
            
            Detected Tech Stack: %s
            Folder Structure:
            %s
            
            Perform the following 6 tasks based on the provided project information.
            
            TASK 1: summary
            Write a concise, professional 2-3 sentence executive summary of what this project is and its architecture. 
            Aggressively infer the business purpose from the file/folder names. Do NOT use conversational filler. Get straight to the point.
            
            TASK 2: annotatedTree
            Reproduce the exact folder structure provided, but append a short, 1-sentence comment to the right of the most important folders/files explaining their core purpose based on standard conventions.
            
            TASK 3: healthCheck
            Identify the likely architectural pattern (e.g., MVC, Layered, Microservices). Perform a structural health check pointing out strengths or missing standard elements (e.g., missing test folders, CI/CD, Docker files).
            Format as a markdown bulleted list. Do NOT use first-person pronouns; state facts objectively.
            
            TASK 4: readme
            Create a professional, production-ready README in standard Markdown. 
            Include these sections: 1. Project Title (%s) & Description, 2. Architecture Overview, 3. Technologies Used, 4. Project Structure, 5. Getting Started (Provide standard generic steps based on the tech stack).
            Output the raw Markdown text. Do NOT wrap your response in markdown formatting blocks. Start directly with the # Title.
            
            TASK 5: resumeBullets
            Write 3 highly professional, action-oriented resume bullet points using the XYZ format: "Accomplished [X] as measured by [Y], by doing [Z]" that a developer could put on their resume for this project. Format as a markdown list.
            
            TASK 6: interviewQuestions
            Provide 3 technical interview questions a hiring manager is likely to ask about this specific codebase, based on its tech stack and architecture. Format as a markdown list.
            
            CRITICAL CONSTRAINTS:
            - You MUST respond ONLY with a raw, valid JSON object. Do NOT wrap it in markdown code blocks (no ```json).
            - Ensure all markdown formatting inside the JSON strings uses escaped newlines (\\n) and properly escaped quotes (\\") to maintain strict, valid JSON syntax.
            
            JSON SCHEMA:
            {
              "summary": "string",
              "annotatedTree": "string",
              "healthCheck": "string",
              "readme": "string",
              "resumeBullets": "string",
              "interviewQuestions": "string"
            }
            """.formatted(cleanRepoName, techStack.toString(), projectTree, cleanRepoName);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            String responseString = restClient.post()
                    .uri(geminiUrl)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode responseNode = mapper.readTree(responseString);

            if (responseNode != null && responseNode.has("candidates")) {
                String aiJsonString = responseNode.get("candidates").get(0)
                        .get("content").get("parts").get(0).get("text").asText();
                
                aiJsonString = aiJsonString.replace("```json", "").replace("```", "").trim();
                return mapper.readValue(aiJsonString, MasterAiResponse.class);
            }
        } catch (Exception e) {
            System.err.println("Master AI generation failed: " + e.getMessage());
        }
        
        return new MasterAiResponse(
                "Analysis unavailable.", "Tree unavailable.", "Health check unavailable.", 
                "# README Unavailable", "Bullets unavailable.", "Questions unavailable."
        );
    }
}