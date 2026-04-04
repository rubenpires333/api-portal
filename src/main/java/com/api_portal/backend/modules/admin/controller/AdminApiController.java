package com.api_portal.backend.modules.admin.controller;

import com.api_portal.backend.modules.api.dto.ApiResponse;
import com.api_portal.backend.modules.api.service.ApiService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/apis")
@RequiresPermission("admin.apis.manage")
@RequiredArgsConstructor
@Tag(name = "Admin - APIs", description = "Gerenciamento de APIs pelo administrador")
public class AdminApiController {
    
    private final ApiService apiService;
    
    @GetMapping("/pending")
    @Operation(
        summary = "Listar APIs pendentes de aprovação",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Page<ApiResponse>> getPendingApis(
            @PageableDefault(size = 20, sort = "requestedApprovalAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(apiService.getPendingApis(pageable));
    }
    
    @GetMapping("/pending/count")
    @Operation(
        summary = "Contar APIs pendentes de aprovação",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Long> countPendingApis() {
        return ResponseEntity.ok(apiService.countPendingApis());
    }
    
    @PatchMapping("/{id}/approve")
    @Operation(
        summary = "Aprovar API",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse> approveApi(
            @PathVariable UUID id,
            Authentication authentication) {
        String adminId = getUserId(authentication);
        return ResponseEntity.ok(apiService.approveApi(id, adminId));
    }
    
    @PatchMapping("/{id}/reject")
    @Operation(
        summary = "Rejeitar API",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse> rejectApi(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String adminId = getUserId(authentication);
        String reason = body.getOrDefault("reason", "Não especificado");
        return ResponseEntity.ok(apiService.rejectApi(id, adminId, reason));
    }
    
    private String getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
