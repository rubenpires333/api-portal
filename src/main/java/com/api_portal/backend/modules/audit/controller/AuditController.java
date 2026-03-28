package com.api_portal.backend.modules.audit.controller;

import com.api_portal.backend.modules.audit.domain.AuditLog;
import com.api_portal.backend.modules.audit.service.AuditService;
import com.api_portal.backend.shared.security.RequiresPermission;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Logs de auditoria do sistema")
@RequiresPermission("audit.read")
public class AuditController {
    
    private final AuditService auditService;
    
    @GetMapping
    @Operation(
        summary = "Listar todos os logs de auditoria com filtros",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<AuditLog> logs = auditService.searchLogs(userEmail, endpoint, method, statusCode, start, end, pageable);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Obter log por ID",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<AuditLog> getLogById(@PathVariable UUID id) {
        AuditLog log = auditService.getLogById(id);
        return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Listar logs por usuário",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getLogsByUser(
            @PathVariable String userId,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByUserId(userId, pageable));
    }
    
    @GetMapping("/endpoint")
    @Operation(
        summary = "Listar logs por endpoint",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getLogsByEndpoint(
            @RequestParam String endpoint,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByEndpoint(endpoint, pageable));
    }
    
    @GetMapping("/period")
    @Operation(
        summary = "Listar logs por período",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<AuditLog>> getLogsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByPeriod(start, end, pageable));
    }
    
    @GetMapping("/unique-users")
    @Operation(
        summary = "Listar utilizadores únicos nos logs",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<String>> getUniqueUsers() {
        return ResponseEntity.ok(auditService.getUniqueUsers());
    }
    
    @GetMapping("/unique-endpoints")
    @Operation(
        summary = "Listar endpoints únicos nos logs",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<String>> getUniqueEndpoints() {
        return ResponseEntity.ok(auditService.getUniqueEndpoints());
    }
}
