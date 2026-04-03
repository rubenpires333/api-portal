package com.api_portal.backend.modules.metrics.domain.repository;

import com.api_portal.backend.modules.metrics.domain.entity.ApiMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ApiMetricRepository extends JpaRepository<ApiMetric, UUID> {
    
    List<ApiMetric> findByApiIdAndCreatedAtBetween(UUID apiId, LocalDateTime start, LocalDateTime end);
    
    List<ApiMetric> findByConsumerIdAndCreatedAtBetween(UUID consumerId, LocalDateTime start, LocalDateTime end);
    
    List<ApiMetric> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByApiIdAndCreatedAtAfter(UUID apiId, LocalDateTime after);
    
    long countByCreatedAtAfter(LocalDateTime after);
    
    @Query("SELECT COUNT(m) FROM ApiMetric m WHERE m.apiId = :apiId AND m.statusCode >= 400 AND m.createdAt >= :after")
    long countErrorsByApiIdAndCreatedAtAfter(@Param("apiId") UUID apiId, @Param("after") LocalDateTime after);
    
    @Query("SELECT AVG(m.responseTimeMs) FROM ApiMetric m WHERE m.apiId = :apiId AND m.createdAt >= :after")
    Double getAverageResponseTimeByApiIdAndCreatedAtAfter(@Param("apiId") UUID apiId, @Param("after") LocalDateTime after);
    
    @Query("SELECT COUNT(DISTINCT m.consumerId) FROM ApiMetric m WHERE m.apiId = :apiId AND m.createdAt >= :after")
    long countUniqueConsumersByApiIdAndCreatedAtAfter(@Param("apiId") UUID apiId, @Param("after") LocalDateTime after);
    
    @Query("SELECT m.apiId, COUNT(m) as callCount FROM ApiMetric m WHERE m.createdAt >= :after GROUP BY m.apiId ORDER BY callCount DESC")
    List<Object[]> findTopApisByCallCount(@Param("after") LocalDateTime after);
}
