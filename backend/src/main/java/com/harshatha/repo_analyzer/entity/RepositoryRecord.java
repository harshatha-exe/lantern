package com.harshatha.repo_analyzer.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "repositories")
public class RepositoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // DELETED the raw UUID userId field!

    @Column(name = "github_url", nullable = false, length = 512)
    private String githubUrl;

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, CLONING, ANALYZING, COMPLETED, FAILED

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;
    
    @OneToOne(mappedBy = "repositoryRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Analysis analysis;

    // This is the ONLY mapping to the user_id column now
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- getters and setters ---
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Analysis getAnalysis() { return analysis; }
    public void setAnalysis(Analysis analysis) { this.analysis = analysis; }
 
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    // DELETED getUserId() and setUserId()

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
}