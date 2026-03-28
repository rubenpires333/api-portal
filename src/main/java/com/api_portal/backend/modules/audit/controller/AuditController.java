package com.api_portal.backend.modules.audit.controller;

import com.api_portal.backend.modules.audit.domain.AuditLog;
import com.api_portal.backend.modules.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Logs de auditoria do sistema")
public class AuditController {
    
    private final AuditService auditService;
    
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Listar todos os logs de auditoria (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.getAllLogs(pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Obter log por ID (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<AuditLog> getLogById(@PathVariable UUID id) {
        AuditLog log = auditService.getLogById(id);
        return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Listar logs por usuário (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getLogsByUser(
            @PathVariable String userId,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByUserId(userId, pageable));
    }
    
    @GetMapping("/endpoint")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Listar logs por endpoint (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getLogsByEndpoint(
            @RequestParam String endpoint,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByEndpoint(endpoint, pageable));
    }
    
    @GetMapping("/period")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Listar logs por período (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getLogsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByPeriod(start, end, pageable));
    }
}
