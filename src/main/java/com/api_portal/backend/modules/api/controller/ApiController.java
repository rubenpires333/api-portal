package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.dto.ApiRequest;
import com.api_portal.backend.modules.api.dto.ApiResponse;
import com.api_portal.backend.modules.api.service.ApiService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apis")
@RequiredArgsConstructor
@Tag(name = "APIs", description = "Gerenciamento de APIs")
public class ApiController {
    
    private final ApiService apiService;
    
    @GetMapping
    @Operation(summary = "Listar todas as APIs públicas")
    public ResponseEntity<Page<ApiResponse>> getAllApis(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(apiService.getAllApis(pageable));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Buscar APIs")
    public ResponseEntity<Page<ApiResponse>> searchApis(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(apiService.searchApis(q, pageable));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de uma API")
    public ResponseEntity<ApiResponse> getApiById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiService.getApiById(id));
    }
    
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Obter API por slug")
    public ResponseEntity<ApiResponse> getApiBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(apiService.getApiBySlug(slug));
    }
    
    @GetMapping("/my")
    @RequiresPermission("api.read")
    @Operation(
        summary = "Listar minhas APIs",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<ApiResponse>> getMyApis(Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(apiService.getMyApis(providerId));
    }
    
    @PostMapping
    @RequiresPermission("api.create")
    @Operation(
        summary = "Criar nova API",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse> createApi(
            @Valid @RequestBody ApiRequest request,
            Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String providerId = jwt.getSubject();
        String providerName = jwt.getClaimAsString("name");
        String providerEmail = jwt.getClaimAsString("email");
        
        ApiResponse response = apiService.createApi(request, providerId, providerName, providerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("api.update")
    @Operation(
        summary = "Atualizar API",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse> updateApi(
            @PathVariable UUID id,
            @Valid @RequestBody ApiRequest request,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(apiService.updateApi(id, request, providerId));
    }
    
    @PatchMapping("/{id}/publish")
    @RequiresPermission("api.publish")
    @Operation(
        summary = "Publicar API",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse> publishApi(
            @PathVariable UUID id,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(apiService.publishApi(id, providerId));
    }
    
    @PatchMapping("/{id}/deprecate")
    @RequiresPermission("api.update")
    @Operation(
        summary = "Depreciar API",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse> deprecateApi(
            @PathVariable UUID id,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        return ResponseEntity.ok(apiService.deprecateApi(id, providerId));
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("api.delete")
    @Operation(
        summary = "Deletar API",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Void> deleteApi(
            @PathVariable UUID id,
            Authentication authentication) {
        String providerId = getUserId(authentication);
        apiService.deleteApi(id, providerId);
        return ResponseEntity.noContent().build();
    }
    
    private String getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
