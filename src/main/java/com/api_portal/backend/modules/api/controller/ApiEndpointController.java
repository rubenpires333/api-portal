package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.dto.ApiEndpointRequest;
import com.api_portal.backend.modules.api.dto.ApiEndpointResponse;
import com.api_portal.backend.modules.api.service.ApiEndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/versions/{versionId}/endpoints")
@RequiredArgsConstructor
@Tag(name = "API Endpoints", description = "Gerenciamento de endpoints de APIs")
public class ApiEndpointController {
    
    private final ApiEndpointService endpointService;
    
    @GetMapping
    @Operation(summary = "Listar endpoints de uma versão")
    public ResponseEntity<List<ApiEndpointResponse>> getEndpoints(@PathVariable UUID versionId) {
        return ResponseEntity.ok(endpointService.getEndpointsByVersionId(versionId));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de um endpoint")
    public ResponseEntity<ApiEndpointResponse> getEndpointById(@PathVariable UUID id) {
        return ResponseEntity.ok(endpointService.getEndpointById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN')")
    @Operation(
        summary = "Criar novo endpoint",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiEndpointResponse> createEndpoint(
            @PathVariable UUID versionId,
            @Valid @RequestBody ApiEndpointRequest request,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        ApiEndpointResponse response = endpointService.createEndpoint(versionId, request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN')")
    @Operation(
        summary = "Atualizar endpoint",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiEndpointResponse> updateEndpoint(
            @PathVariable UUID id,
            @Valid @RequestBody ApiEndpointRequest request,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(endpointService.updateEndpoint(id, request, providerId));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN')")
    @Operation(
        summary = "Deletar endpoint",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Void> deleteEndpoint(
            @PathVariable UUID id,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        endpointService.deleteEndpoint(id, providerId);
        return ResponseEntity.noContent().build();
    }
    
    private String getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
