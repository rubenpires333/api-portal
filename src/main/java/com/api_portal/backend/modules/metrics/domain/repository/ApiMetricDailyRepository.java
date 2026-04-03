package com.api_portal.backend.modules.metrics.domain.repository;

import com.api_portal.backend.modules.metrics.domain.entity.ApiMetricDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiMetricDailyRepository extends JpaRepository<ApiMetricDaily, UUID> {
    
    Optional<ApiMetricDaily> findByApiIdAndMetricDate(UUID apiId, LocalDate metricDate);
    
    List<ApiMetricDaily> findByApiIdAndMetricDateBetween(UUID apiId, LocalDate start, LocalDate end);
    
    List<ApiMetricDaily> findByMetricDateBetween(LocalDate start, LocalDate end);
    
    @Query("SELECT SUM(m.totalCalls) FROM ApiMetricDaily m WHERE m.metricDate >= :after")
    Long getTotalCallsAfter(@Param("after") LocalDate after);
    
    @Query("SELECT SUM(m.errorCalls) FROM ApiMetricDaily m WHERE m.metricDate >= :after")
    Long getTotalErrorsAfter(@Param("after") LocalDate after);
    
    @Query("SELECT AVG(m.avgResponseTime) FROM ApiMetricDaily m WHERE m.metricDate >= :after")
    Double getAverageResponseTimeAfter(@Param("after") LocalDate after);
    
    @Query("SELECT COUNT(DISTINCT m.apiId) FROM ApiMetricDaily m WHERE m.metricDate >= :after AND m.totalCalls > 0")
    long countActiveApisAfter(@Param("after") LocalDate after);
    
    @Query("SELECT m.apiId, SUM(m.totalCalls) as totalCalls FROM ApiMetricDaily m WHERE m.metricDate >= :after GROUP BY m.apiId ORDER BY totalCalls DESC")
    List<Object[]> findTopApisByTotalCalls(@Param("after") LocalDate after);
}
