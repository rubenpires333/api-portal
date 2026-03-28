package com.api_portal.backend.modules.audit.service;

import com.api_portal.backend.modules.audit.domain.AuditLog;
import com.api_portal.backend.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Async
    @Transactional
    public void logRequest(
            String userId,
            String userEmail,
            String method,
            String uri,
            String queryParams,
            String ipAddress,
            String userAgent,
            Integer statusCode,
            Long executionTime,
            String requestBody,
            String responseBody,
            String errorMessage,
            String stackTrace) {
        
        try {
            AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .userEmail(userEmail)
                .method(method)
                .endpoint(uri)
                .queryParams(queryParams)
                .requestBody(truncate(requestBody, 5000))
                .responseBody(truncate(responseBody, 5000))
                .statusCode(statusCode)
                .executionTime(executionTime)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .errorMessage(truncate(errorMessage, 1000))
                .stackTrace(truncate(stackTrace, 5000))
                .build();
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Erro ao salvar log de auditoria: {}", e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> searchLogs(
            String userEmail,
            String endpoint,
            String method,
            Integer statusCode,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        Specification<AuditLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (userEmail != null && !userEmail.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("userEmail"), userEmail));
            }
            
            if (endpoint != null && !endpoint.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("endpoint"), endpoint));
            }
            
            if (method != null && !method.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("method"), method));
            }
            
            if (statusCode != null) {
                predicates.add(criteriaBuilder.equal(root.get("statusCode"), statusCode));
            }
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return auditLogRepository.findAll(spec, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByUserId(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByEndpoint(String endpoint, Pageable pageable) {
        return auditLogRepository.findByEndpointContaining(endpoint, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByPeriod(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(start, end, pageable);
    }
    
    @Transactional(readOnly = true)
    public AuditLog getLogById(UUID id) {
        return auditLogRepository.findById(id).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<String> getUniqueUsers() {
        return auditLogRepository.findDistinctUserEmails();
    }
    
    @Transactional(readOnly = true)
    public List<String> getUniqueEndpoints() {
        return auditLogRepository.findDistinctEndpoints();
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
