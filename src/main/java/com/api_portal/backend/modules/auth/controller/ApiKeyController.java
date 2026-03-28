package com.api_portal.backend.modules.auth.controller;

import com.api_portal.backend.modules.auth.dto.ApiKeyRequest;
import com.api_portal.backend.modules.auth.dto.ApiKeyResponse;
import com.api_portal.backend.modules.auth.model.ApiKey;
import com.api_portal.backend.modules.auth.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Gestão de API Keys para autenticação")
@SecurityRequirement(name = "Bearer Authentication")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @PostMapping
    @Operation(
        summary = "Criar API Key",
        description = "Cria uma nova API Key para o utilizador autenticado"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "API Key criada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @Valid @RequestBody ApiKeyRequest request,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        
        ApiKey apiKey = apiKeyService.createApiKey(
            userId,
            request.getName(),
            request.getDescription(),
            request.getExpiresInDays()
        );
        
        ApiKeyResponse response = mapToResponse(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(
        summary = "Listar API Keys",
        description = "Lista todas as API Keys do utilizador autenticado"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista obtida com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(Authentication authentication) {
        String userId = getUserId(authentication);
        
        List<ApiKeyResponse> apiKeys = apiKeyService.getUserApiKeys(userId)
            .stream()
            .map(this::mapToResponseWithoutKey)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(apiKeys);
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Revogar API Key",
        description = "Revoga (desativa) uma API Key"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "API Key revogada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "API Key não encontrada")
    })
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable Long id,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        apiKeyService.revokeApiKey(id, userId);
        
        return ResponseEntity.noContent().build();
    }
    
    private String getUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }
    
    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
            .id(apiKey.getId())
            .keyValue(apiKey.getKeyValue())
            .name(apiKey.getName())
            .description(apiKey.getDescription())
            .active(apiKey.getActive())
            .createdAt(apiKey.getCreatedAt())
            .expiresAt(apiKey.getExpiresAt())
            .lastUsedAt(apiKey.getLastUsedAt())
            .build();
    }
    
    private ApiKeyResponse mapToResponseWithoutKey(ApiKey apiKey) {
        return ApiKeyResponse.builder()
            .id(apiKey.getId())
            .keyValue(null) // Não retornar o valor da chave na listagem
            .name(apiKey.getName())
            .description(apiKey.getDescription())
            .active(apiKey.getActive())
            .createdAt(apiKey.getCreatedAt())
            .expiresAt(apiKey.getExpiresAt())
            .lastUsedAt(apiKey.getLastUsedAt())
            .build();
    }
}
