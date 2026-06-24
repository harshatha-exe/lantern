package com.harshatha.repo_analyzer.dto;

import java.util.HashSet;
import java.util.Set;

public record TechStackReport(
        Set<String> languages,
        Set<String> frameworks,
        Set<String> databases,
        Set<String> tools
) {
    // A convenient static factory method to initialize empty sets
    public static TechStackReport createEmpty() {
        return new TechStackReport(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }
}