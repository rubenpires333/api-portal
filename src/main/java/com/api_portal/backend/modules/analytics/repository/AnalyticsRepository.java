package com.api_portal.backend.modules.analytics.repository;

import com.api_portal.backend.modules.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<AuditLog, UUID> {
    
    @Query("""
        SELECT COUNT(a) FROM AuditLog a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.timestamp BETWEEN :start AND :end
    """)
    Long countTotalRequests(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("""
        SELECT COUNT(a) FROM AuditLog a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.statusCode >= 400 
        AND a.timestamp BETWEEN :start AND :end
    """)
    Long countTotalErrors(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("""
        SELECT AVG(a.executionTime) FROM AuditLog a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.executionTime IS NOT NULL 
        AND a.timestamp BETWEEN :start AND :end
    """)
    Double calculateAverageLatency(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("""
        SELECT a.userEmail, COUNT(a) as count 
        FROM AuditLog a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.userEmail IS NOT NULL 
        AND a.timestamp BETWEEN :start AND :end 
        GROUP BY a.userEmail 
        ORDER BY count DESC
    """)
    List<Object[]> findTopConsumers(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("limit") int limit
    );
    
    @Query(value = """
        SELECT a.endpoint, a.method, COUNT(*) as count, AVG(a.execution_time) as avg_latency 
        FROM audit_logs a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.timestamp BETWEEN :start AND :end 
        GROUP BY a.endpoint, a.method 
        ORDER BY count DESC 
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopEndpoints(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("limit") int limit
    );
    
    @Query("""
        SELECT a.method, COUNT(a) as count 
        FROM AuditLog a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.timestamp BETWEEN :start AND :end 
        GROUP BY a.method 
        ORDER BY count DESC
    """)
    List<Object[]> countRequestsByMethod(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("""
        SELECT 
            CASE 
                WHEN a.statusCode < 300 THEN 'success'
                WHEN a.statusCode < 400 THEN 'redirect'
                WHEN a.statusCode < 500 THEN 'client_error'
                ELSE 'server_error'
            END as status,
            COUNT(a) as count 
        FROM AuditLog a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.timestamp BETWEEN :start AND :end 
        GROUP BY status
    """)
    List<Object[]> countRequestsByStatus(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query(value = """
        SELECT DATE(a.timestamp) as day, COUNT(*) as count 
        FROM audit_logs a 
        WHERE a.endpoint LIKE '/gateway/api/%' 
        AND a.timestamp BETWEEN :start AND :end 
        GROUP BY day 
        ORDER BY day
    """, nativeQuery = true)
    List<Object[]> countRequestsByDay(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
