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
        // Initialize the modern Spring Boot 3 RestClient
        this.restClient = RestClient.create();
    }

    public String generateProjectTree(Path repoPath) {
        StringBuilder tree = new StringBuilder();
        try (Stream<Path> paths = Files.walk(repoPath, 5)) { // Go 5 levels deep
            paths.filter(p -> {
                String pathStr = p.toString();
                // Ignore dependency and hidden folders
                return !pathStr.contains(".git") && !pathStr.contains("node_modules") 
                    && !pathStr.contains("target") && !pathStr.contains("build")
                    && !pathStr.contains(".idea") && !pathStr.contains("dist");
            })
            .limit(100) // Keep it to 100 items so we don't overwhelm the AI
            .forEach(p -> {
                int depth = repoPath.relativize(p).getNameCount();
                String indent = "  ".repeat(Math.max(0, depth - 1));
                String marker = Files.isDirectory(p) ? "📁 " : "📄 ";
                tree.append(indent).append(marker).append(p.getFileName()).append("\n");
            });
        } catch (Exception e) {
            return "Could not map directory structure.";
        }
        return tree.toString();
    }

    public MasterAiResponse generateMasterAnalysis(TechStackReport techStack, String projectTree) {
        String prompt = """
            You are a Senior Software Architect and Expert Technical Recruiter analyzing a codebase.
            
            Detected Tech Stack: %s
            Folder Structure:
            %s
            
            Perform the following 6 tasks based on the provided project information.
            
            TASK 1: summary
            Write a concise, professional 2-3 sentence executive summary of what this project is and its architecture. 
            Aggressively infer the business purpose from the file/folder names. Do NOT use conversational filler (e.g., "This project is..."). Get straight to the point.
            
            TASK 2: annotatedTree
            Reproduce the exact folder structure provided, but append a short, 1-sentence comment to the right of the most important folders/files explaining their core purpose based on standard conventions.
            
            TASK 3: healthCheck
            Identify the likely architectural pattern (e.g., MVC, Layered, Microservices). Perform a structural health check pointing out strengths or missing standard elements (e.g., missing test folders, CI/CD, Docker files).
            Format as a markdown bulleted list. Do NOT use first-person pronouns; state facts objectively (e.g., "The absence of a test folder indicates...").
            
            TASK 4: readme
            Create a professional, production-ready README in standard Markdown. 
            Include these sections: 1. Project Title & Description, 2. Architecture Overview, 3. Technologies Used, 4. Project Structure, 5. Getting Started (Provide standard generic steps based on the tech stack, e.g., 'npm install' or 'mvn clean install').
            Output the raw Markdown text. Do NOT wrap your response in markdown formatting blocks (like ```markdown). Start directly with the # Title.
            
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
            """.formatted(techStack.toString(), projectTree);

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
                
                // Cleanup markdown blocks if the AI disobeys
                aiJsonString = aiJsonString.replace("```json", "").replace("```", "").trim();
                
                return mapper.readValue(aiJsonString, MasterAiResponse.class);
            }
        } catch (Exception e) {
            System.err.println("Master AI generation failed: " + e.getMessage());
        }
        
        // Fallback if the single call fails
        return new MasterAiResponse(
                "Analysis unavailable.", "Tree unavailable.", "Health check unavailable.", 
                "# README Unavailable", "Bullets unavailable.", "Questions unavailable."
        );
    }

    /* ==========================================================================================
       COMMENTED OUT PREVIOUS IMPLEMENTATIONS (Kept for reference/history)
       ========================================================================================== 

    public String generateRepositorySummary(Path repoPath, TechStackReport techStack) {
        String readmeContent = extractReadme(repoPath);
        String prompt = buildPrompt(readmeContent, techStack);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );
        
        try {
            String responseString = restClient.post()
                    .uri(geminiUrl)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode response = mapper.readTree(responseString);

            if (response != null && response.has("candidates")) {
                return response.get("candidates").get(0)
                        .get("content")
                        .get("parts").get(0)
                        .get("text").asText();
            }
            
            System.out.println("DEBUG RAW GOOGLE RESPONSE: " + responseString);
            return "Analysis completed, but AI failed to return a readable summary.";

        } catch (Exception e) {
            System.err.println("Gemini API call failed: " + e.getMessage());
            return "AI Summary unavailable due to an API error.";
        }
    }
    
    private String extractReadme(Path repoPath) {
        try {
            Path readme = repoPath.resolve("README.md");
            if (!Files.exists(readme)) readme = repoPath.resolve("readme.md");
            
            if (Files.exists(readme)) {
                String content = Files.readString(readme);
                return content.length() > 3000 ? content.substring(0, 3000) + "..." : content;
            }

            return buildFallbackFileTree(repoPath);

        } catch (Exception e) {
            System.out.println("Could not read README or directory structure.");
            return "No README file found.";
        }
    }

    private String buildFallbackFileTree(Path repoPath) {
        StringBuilder treeContext = new StringBuilder();
        treeContext.append("No README found. Infer the project's business purpose from these core filenames:\n");

        try (Stream<Path> paths = Files.walk(repoPath, 4)) {
            paths.filter(p -> {
                    String pathStr = p.toString();
                    return !pathStr.contains(".git") 
                        && !pathStr.contains("node_modules") 
                        && !pathStr.contains("target") 
                        && !pathStr.contains(".idea");
                })
                .filter(Files::isRegularFile)
                .limit(40)
                .forEach(p -> {
                    treeContext.append("- ").append(repoPath.relativize(p)).append("\n");
                });
        } catch (Exception e) {
            return "No README file found.";
        }

        return treeContext.toString();
    }

    private String buildPrompt(String contextSnippet, TechStackReport techStack) {
        return \"\"\"
            You are an expert Senior Software Architect reviewing a GitHub repository.
            Based on the detected technology stack and the provided Repository Context below (which will either be a README snippet OR a raw file tree), write a concise, professional 2-3 sentence summary of what this project is and what it does.
            
            CRITICAL INSTRUCTIONS:
            - If the context is a file tree, DO NOT complain that the README is missing. 
            - Aggressively infer the business purpose from the file/folder names (e.g., if you see 'cart' or 'product', call it an e-commerce platform).
            - Do NOT use conversational filler (like "This project is..."). Get straight to the point.
            
            Detected Languages: %s
            Detected Frameworks: %s
            Detected Databases: %s
            Detected Tools: %s
            
            Repository Context:
            %s
            \"\"\".formatted(
                techStack.languages(),
                techStack.frameworks(),
                techStack.databases(),
                techStack.tools(),
                contextSnippet
        );
    }
    
    public StructureAnalysisResult analyzeStructureAndHealth(String projectTree, TechStackReport techStack) {
        String prompt = \"\"\"
            You are an expert Software Architect reviewing a project's structure.
            
            Tech Stack: %s
            Folder Structure:
            %s
            
            Perform two tasks:
            1. Annotated Map: Reproduce the exact folder structure provided, but append a short, 1-sentence comment to the right of the most important folders/files explaining their core purpose based on standard conventions.
            2. Structural Health Check: Identify the likely architectural pattern (e.g., MVC, Layered) and perform a structural health check pointing out strengths or missing standard elements (e.g., missing test folders, missing CI/CD, missing docker files).
            
            CRITICAL CONSTRAINTS:
            - The Health Check MUST be formatted as a markdown bulleted list.
            - Do NOT use first-person pronouns (e.g., do not use "I", "my", "noticed", "see"). State facts objectively (e.g., "The absence of a test folder indicates...").
            - You MUST respond ONLY with a raw, valid JSON object. Do not wrap it in markdown code blocks.
            
            JSON Schema:
            {
              "annotatedTree": "string (the annotated folder tree)",
              "healthCheck": "string (the bulleted health check list)"
            }
            \"\"\".formatted(techStack.frameworks(), projectTree);

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

            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode responseNode = mapper.readTree(responseString);

            if (responseNode != null && responseNode.has("candidates")) {
                String aiJsonString = responseNode.get("candidates").get(0)
                        .get("content").get("parts").get(0).get("text").asText();
                
                aiJsonString = aiJsonString.replace("```json", "").replace("```", "").trim();
                return mapper.readValue(aiJsonString, StructureAnalysisResult.class);
            }
        } catch (Exception e) {
            System.err.println("Architecture analysis failed: " + e.getMessage());
        }
        
        return new StructureAnalysisResult("Tree analysis failed.", "Health check unavailable.");
    }

    public String generateReadme(String summary, TechStackReport techStack, String architecture, String projectTree) {
        String prompt = \"\"\"
            You are a Senior Developer writing documentation. Create a professional, production-ready README.md file in standard Markdown format for this project.
            
            Project Summary: %s
            Tech Stack: %s
            Architecture & Health: %s
            Folder Structure:
            %s
            
            Please include the following sections:
            1. Project Title & Description
            2. Architecture Overview
            3. Technologies Used
            4. Project Structure
            5. Getting Started (Provide standard generic steps based on the tech stack, e.g., 'npm install' or 'mvn clean install')
            
            CRITICAL INSTRUCTION: Output ONLY the raw Markdown text. Do NOT wrap your response in markdown formatting blocks (like ```markdown). Start directly with the # Title.
            \"\"\".formatted(summary, techStack.frameworks(), architecture, projectTree);

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
                String markdown = responseNode.get("candidates").get(0)
                        .get("content").get("parts").get(0).get("text").asText();
                
                return markdown.replace("```markdown", "").replace("```", "").trim();
            }
            return "# README Generation Failed";
        } catch (Exception e) {
            System.err.println("README generation failed: " + e.getMessage());
            return "# README Unavailable";
        }
    }

    public JobHunterResult generateJobHunterAssets(String summary, TechStackReport techStack) {
        String prompt = \"\"\"
            You are an Expert Technical Recruiter and Senior Engineering Manager. Review this project:
            
            Tech Stack: %s
            Project Summary: %s
            
            Perform two tasks:
            1. Resume Bullets: Write 3 highly professional, action-oriented resume bullet points (using the XYZ format: "Accomplished [X] as measured by [Y], by doing [Z]") that a developer could put on their resume for this project. Format as a markdown list.
            2. Interview Questions: Provide 3 technical interview questions a hiring manager is likely to ask about this specific codebase, based on its tech stack and architecture. Format as a markdown list.
            
            CRITICAL INSTRUCTION: You MUST respond ONLY with a raw, valid JSON object. Do not wrap it in markdown code blocks (no ```json).
            
            JSON Schema:
            {
              "resumeBullets": "string (the markdown list of bullets)",
              "interviewQuestions": "string (the markdown list of questions)"
            }
            \"\"\".formatted(techStack.frameworks(), summary);

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
                return mapper.readValue(aiJsonString, JobHunterResult.class);
            }
        } catch (Exception e) {
            System.err.println("Job Hunter generation failed: " + e.getMessage());
        }
        
        return new JobHunterResult("Bullets unavailable.", "Questions unavailable.");
    }
    
    ========================================================================================== */
}