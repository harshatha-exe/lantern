package com.harshatha.repo_analyzer.entity;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", referencedColumnName = "id", nullable = false, unique = true)
    @JsonIgnore
    private RepositoryRecord repositoryRecord;


    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String techStack; 

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String architecturePattern;  

    @Column(columnDefinition = "TEXT")
    private String projectStructure; 

    @Column(columnDefinition = "TEXT")
    private String generatedReadme;

    @Column(name = "total_files")
    private Integer totalFiles;

    @Column(name = "total_size_kb")
    private Long totalSizeKb;

    @Column(columnDefinition = "TEXT")
    private String resumeBullets;

    @Column(columnDefinition = "TEXT")
    private String interviewQuestions;

    // ... Getters and Setters ...

    public String getResumeBullets() { return resumeBullets; }
    public void setResumeBullets(String resumeBullets) { this.resumeBullets = resumeBullets; }

    public String getInterviewQuestions() { return interviewQuestions; }
    public void setInterviewQuestions(String interviewQuestions) { this.interviewQuestions = interviewQuestions;}

    public Integer getTotalFiles() { return totalFiles; }
    public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }

    public Long getTotalSizeKb() { return totalSizeKb; }
    public void setTotalSizeKb(Long totalSizeKb) { this.totalSizeKb = totalSizeKb; }

    public String getGeneratedReadme() { return generatedReadme; }
    public void setGeneratedReadme(String generatedReadme) { this.generatedReadme = generatedReadme; }

    public String getArchitecturePattern() { return architecturePattern; }
    public void setArchitecturePattern(String architecturePattern) { this.architecturePattern = architecturePattern; }
    public String getProjectStructure() { return projectStructure; }
    public void setProjectStructure(String projectStructure) { this.projectStructure = projectStructure; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public RepositoryRecord getRepositoryRecord() { return repositoryRecord; }
    public void setRepositoryRecord(RepositoryRecord repositoryRecord) { this.repositoryRecord = repositoryRecord; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
}