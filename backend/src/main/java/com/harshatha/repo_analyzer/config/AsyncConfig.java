package com.harshatha.repo_analyzer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // This simple annotation tells Spring to look for @Async methods 
    // and run them in a separate thread pool automatically.
}