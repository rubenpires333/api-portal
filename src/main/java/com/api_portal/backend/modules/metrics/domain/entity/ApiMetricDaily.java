package com.api_portal.backend.modules.metrics.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_metrics_daily", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"api_id", "metric_date"}),
    indexes = {
        @Index(name = "idx_api_metrics_daily_api_id", columnList = "api_id"),
        @Index(name = "idx_api_metrics_daily_date", columnList = "metric_date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiMetricDaily {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "api_id", nullable = false)
    private UUID apiId;
    
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;
    
    @Column(name = "total_calls", nullable = false)
    @Builder.Default
    private Long totalCalls = 0L;
    
    @Column(name = "success_calls", nullable = false)
    @Builder.Default
    private Long successCalls = 0L;
    
    @Column(name = "error_calls", nullable = false)
    @Builder.Default
    private Long errorCalls = 0L;
    
    @Column(name = "avg_response_time", nullable = false)
    @Builder.Default
    private Double avgResponseTime = 0.0;
    
    @Column(name = "min_response_time", nullable = false)
    @Builder.Default
    private Double minResponseTime = 0.0;
    
    @Column(name = "max_response_time", nullable = false)
    @Builder.Default
    private Double maxResponseTime = 0.0;
    
    @Column(name = "total_request_size", nullable = false)
    @Builder.Default
    private Long totalRequestSize = 0L;
    
    @Column(name = "total_response_size", nullable = false)
    @Builder.Default
    private Long totalResponseSize = 0L;
    
    @Column(name = "unique_consumers", nullable = false)
    @Builder.Default
    private Integer uniqueConsumers = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public double getErrorRate() {
        if (totalCalls == 0) return 0.0;
        return (errorCalls * 100.0) / totalCalls;
    }
}
