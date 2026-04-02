package com.api_portal.backend.modules.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
    
    @Column(name = "attempts", nullable = false)
    private Integer attempts;
    
    @Column(name = "last_attempt", nullable = false)
    private LocalDateTime lastAttempt;
    
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;
    
    @Column(name = "requires_captcha", nullable = false)
    private Boolean requiresCaptcha;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
