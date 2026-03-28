package com.api_portal.backend.modules.audit.repository;

import com.api_portal.backend.modules.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    Page<AuditLog> findByUserId(String userId, Pageable pageable);
    
    Page<AuditLog> findByEndpointContaining(String endpoint, Pageable pageable);
    
    Page<AuditLog> findByStatusCode(Integer statusCode, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end")
    Page<AuditLog> findByTimestampBetween(
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end, 
        Pageable pageable
    );
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :start AND :end")
    List<AuditLog> findUserActivityInPeriod(
        @Param("userId") String userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.statusCode >= 400")
    Long countErrors();
    
    @Query("SELECT a.endpoint, COUNT(a) as count FROM AuditLog a GROUP BY a.endpoint ORDER BY count DESC")
    List<Object[]> findMostAccessedEndpoints(Pageable pageable);
}
