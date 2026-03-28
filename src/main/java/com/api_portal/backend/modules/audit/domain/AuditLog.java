package com.api_portal.backend.modules.audit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_endpoint", columnList = "endpoint"),
    @Index(name = "idx_audit_status", columnList = "statusCode")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private String userId;
    
    private String userEmail;
    
    @Column(nullable = false)
    private String method;
    
    @Column(nullable = false, length = 500)
    private String endpoint;
    
    @Column(columnDefinition = "TEXT")
    private String queryParams;
    
    @Column(columnDefinition = "TEXT")
    private String requestBody;
    
    @Column(columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(nullable = false)
    private Integer statusCode;
    
    private Long executionTime;
    
    private String ipAddress;
    
    private String userAgent;
    
    private String errorMessage;
    
    @Column(columnDefinition = "TEXT")
    private String stackTrace;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
