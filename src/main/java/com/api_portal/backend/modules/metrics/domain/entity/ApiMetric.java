package com.api_portal.backend.modules.metrics.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_metrics", indexes = {
    @Index(name = "idx_api_metrics_api_id", columnList = "api_id"),
    @Index(name = "idx_api_metrics_subscription_id", columnList = "subscription_id"),
    @Index(name = "idx_api_metrics_consumer_id", columnList = "consumer_id"),
    @Index(name = "idx_api_metrics_created_at", columnList = "created_at"),
    @Index(name = "idx_api_metrics_status_code", columnList = "status_code"),
    @Index(name = "idx_api_metrics_api_created", columnList = "api_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "api_id", nullable = false)
    private UUID apiId;
    
    @Column(name = "subscription_id")
    private UUID subscriptionId;
    
    @Column(name = "consumer_id")
    private String consumerId;
    
    @Column(name = "consumer_name")
    private String consumerName;
    
    @Column(nullable = false, length = 500)
    private String endpoint;
    
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;
    
    @Column(name = "status_code", nullable = false)
    private Integer statusCode;
    
    @Column(name = "response_time_ms", nullable = false)
    private Double responseTimeMs;
    
    @Column(name = "request_size_bytes")
    private Long requestSizeBytes;
    
    @Column(name = "response_size_bytes")
    private Long responseSizeBytes;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    public boolean isError() {
        return statusCode >= 400;
    }
}
