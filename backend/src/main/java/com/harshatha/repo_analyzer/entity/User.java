package com.harshatha.repo_analyzer.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    // --- UserDetails Interface Methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // Empty for now; add roles later if needed
    }

    @Override
    public String getPassword() {
        return passwordHash; // Spring Security needs this to verify hashes
    }

    @Override
    public String getUsername() {
        return email; // We use email as the primary identifier
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // --- Getters and Setters ---

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Notice standard naming: field is passwordHash -> setter is setPasswordHash
    public void setHashPassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public UUID getId() {
        return id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
}