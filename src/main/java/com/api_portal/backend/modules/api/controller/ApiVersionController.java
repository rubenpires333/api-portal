package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.dto.ApiVersionRequest;
import com.api_portal.backend.modules.api.dto.ApiVersionResponse;
import com.api_portal.backend.modules.api.service.ApiVersionService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apis/{apiId}/versions")
@RequiredArgsConstructor
@Tag(name = "API Versions", description = "Gerenciamento de versões de APIs")
public class ApiVersionController {
    
    private final ApiVersionService versionService;
    
    @GetMapping
    @Operation(summary = "Listar versões de uma API")
    public ResponseEntity<List<ApiVersionResponse>> getVersions(@PathVariable UUID apiId) {
        return ResponseEntity.ok(versionService.getVersionsByApiId(apiId));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de uma versão")
    public ResponseEntity<ApiVersionResponse> getVersionById(@PathVariable UUID id) {
        return ResponseEntity.ok(versionService.getVersionById(id));
    }
    
    @PostMapping
    @RequiresPermission("version.create")
    @Operation(
        summary = "Criar nova versão",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiVersionResponse> createVersion(
            @PathVariable UUID apiId,
            @Valid @RequestBody ApiVersionRequest request,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        ApiVersionResponse response = versionService.createVersion(apiId, request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PatchMapping("/{id}/default")
    @RequiresPermission("version.update")
    @Operation(
        summary = "Definir versão como padrão",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiVersionResponse> setDefaultVersion(
            @PathVariable UUID apiId,
            @PathVariable UUID id,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(versionService.setDefaultVersion(apiId, id, providerId));
    }
    
    @PatchMapping("/{id}/publish")
    @RequiresPermission("version.update")
    @Operation(
        summary = "Publicar versão",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiVersionResponse> publishVersion(
            @PathVariable UUID apiId,
            @PathVariable UUID id,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(versionService.publishVersion(apiId, id, providerId));
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("version.update")
    @Operation(
        summary = "Atualizar versão (apenas se não publicada)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiVersionResponse> updateVersion(
            @PathVariable UUID apiId,
            @PathVariable UUID id,
            @Valid @RequestBody ApiVersionRequest request,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(versionService.updateVersion(apiId, id, request, providerId));
    }
    
    @PatchMapping("/{id}/deprecate")
    @RequiresPermission("version.update")
    @Operation(
        summary = "Depreciar versão",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiVersionResponse> deprecateVersion(
            @PathVariable UUID id,
            @RequestParam(required = false) String message,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(versionService.deprecateVersion(id, message, providerId));
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("version.delete")
    @Operation(
        summary = "Deletar versão",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Void> deleteVersion(
            @PathVariable UUID id,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        versionService.deleteVersion(id, providerId);
        return ResponseEntity.noContent().build();
    }
    
    private String getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
