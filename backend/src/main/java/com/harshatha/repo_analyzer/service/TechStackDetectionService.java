package com.harshatha.repo_analyzer.service;

import com.harshatha.repo_analyzer.dto.TechStackReport;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
public class TechStackDetectionService {

    public TechStackReport detectStack(Path repoPath) {
        TechStackReport report = TechStackReport.createEmpty();

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String str = p.toString().toLowerCase();
                     // Ignore heavy dependencies to speed up the scan
                     return !str.contains("node_modules") && !str.contains(".git") 
                             && !str.contains("target") && !str.contains("venv");
                 })
                 .forEach(file -> {
                     String fileName = file.getFileName().toString();
                     String pathStr = file.toString().toLowerCase();

                     // 1. GUARANTEED LANGUAGE DETECTION (By File Extension)
                     if (pathStr.endsWith(".java")) report.languages().add("Java");
                     else if (pathStr.endsWith(".js") || pathStr.endsWith(".jsx")) report.languages().add("JavaScript");
                     else if (pathStr.endsWith(".ts") || pathStr.endsWith(".tsx")) report.languages().add("TypeScript");
                     else if (pathStr.endsWith(".py")) report.languages().add("Python");
                     else if (pathStr.endsWith(".go")) report.languages().add("Go");
                     else if (pathStr.endsWith(".rb")) report.languages().add("Ruby");
                     else if (pathStr.endsWith(".cs")) report.languages().add("C#");
                     else if (pathStr.endsWith(".cpp") || pathStr.endsWith(".hpp")) report.languages().add("C++");
                     else if (pathStr.endsWith(".php")) report.languages().add("PHP");
                     else if (pathStr.endsWith(".html")) report.languages().add("HTML");

                     // 2. ECOSYSTEM DEEP SCAN (Finds files anywhere in the tree)
                     if (fileName.equals("pom.xml") || fileName.equals("build.gradle")) {
                         analyzeJavaEcosystem(file, report);
                     } else if (fileName.equals("package.json")) {
                         analyzeNodeEcosystem(file, report);
                     } else if (fileName.equals("requirements.txt") || fileName.equals("pipfile")) {
                         analyzePythonEcosystem(file, report);
                     }
                 });

        } catch (Exception e) {
            System.err.println("Error during tech stack detection: " + e.getMessage());
        }

        return report;
    }

    private void analyzeJavaEcosystem(Path buildFile, TechStackReport report) {
        try {
            report.tools().add(buildFile.getFileName().toString().contains("pom") ? "Maven" : "Gradle");
            String content = Files.readString(buildFile).toLowerCase();

            if (content.contains("spring-boot")) report.frameworks().add("Spring Boot");
            if (content.contains("spring-security")) report.frameworks().add("Spring Security");
            if (content.contains("hibernate")) report.frameworks().add("Hibernate");
            if (content.contains("postgresql")) report.databases().add("PostgreSQL");
            if (content.contains("mysql")) report.databases().add("MySQL");
            if (content.contains("h2database")) report.databases().add("H2 Database");
        } catch (Exception ignored) {}
    }

    private void analyzeNodeEcosystem(Path packageJsonPath, TechStackReport report) {
        try {
            report.tools().add("NPM/Yarn");
            String content = Files.readString(packageJsonPath).toLowerCase();

            if (content.contains("\"react\"")) report.frameworks().add("React");
            if (content.contains("\"next\"")) report.frameworks().add("Next.js");
            if (content.contains("\"express\"")) report.frameworks().add("Express.js");
            if (content.contains("\"vue\"")) report.frameworks().add("Vue.js");
            if (content.contains("\"tailwindcss\"")) report.frameworks().add("Tailwind CSS");
            if (content.contains("\"mongoose\"") || content.contains("\"mongodb\"")) report.databases().add("MongoDB");
            if (content.contains("\"prisma\"")) report.tools().add("Prisma ORM");
        } catch (Exception ignored) {}
    }

    private void analyzePythonEcosystem(Path reqFile, TechStackReport report) {
        try {
            report.tools().add("Pip");
            String content = Files.readString(reqFile).toLowerCase();

            if (content.contains("django")) report.frameworks().add("Django");
            if (content.contains("flask")) report.frameworks().add("Flask");
            if (content.contains("fastapi")) report.frameworks().add("FastAPI");
            if (content.contains("psycopg2")) report.databases().add("PostgreSQL");
            if (content.contains("sqlalchemy")) report.tools().add("SQLAlchemy");
        } catch (Exception ignored) {}
    }
}