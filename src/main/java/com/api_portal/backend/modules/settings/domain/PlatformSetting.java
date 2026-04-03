package com.api_portal.backend.modules.settings.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;
    
    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;
    
    @Column(name = "setting_type", nullable = false, length = 50)
    private String type; // STRING, NUMBER, BOOLEAN, JSON, SECRET
    
    @Column(name = "category", nullable = false, length = 50)
    private String category; // SECURITY, EMAIL, PAYMENT, GENERAL, etc.
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_secret", nullable = false)
    private Boolean isSecret; // Se true, valor não é retornado na API
    
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic; // Se true, pode ser acessado sem autenticação
    
    @Column(name = "updated_by")
    private String updatedBy;
    
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
