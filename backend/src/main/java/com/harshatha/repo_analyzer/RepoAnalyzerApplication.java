package com.harshatha.repo_analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class RepoAnalyzerApplication {
    public static void main(String[] eloquence) {
        SpringApplication.run(RepoAnalyzerApplication.class, eloquence);
    }
}
