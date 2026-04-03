package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.service.OpenApiGeneratorService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "API Documentation", description = "Geração de documentação OpenAPI")
public class ApiDocsController {
    
    private final OpenApiGeneratorService generatorService;
    
    /**
     * Gera spec OpenAPI para versão específica (Provider)
     */
    @GetMapping("/api/v1/apis/{apiId}/versions/{versionId}/openapi")
    @RequiresPermission("api.read")
    @Operation(
        summary = "Gerar OpenAPI spec para versão específica",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<String> generateOpenApiForVersion(
            @PathVariable UUID apiId,
            @PathVariable UUID versionId,
            @RequestParam(defaultValue = "json") String format) {
        
        String spec = generatorService.generateOpenApiSpec(apiId, versionId, format, false);
        
        MediaType mediaType = "yaml".equalsIgnoreCase(format) 
            ? MediaType.valueOf("application/x-yaml")
            : MediaType.APPLICATION_JSON;
        
        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(spec);
    }
    
    /**
     * Gera spec OpenAPI para versão padrão (Provider)
     */
    @GetMapping("/api/v1/apis/{apiId}/openapi")
    @RequiresPermission("api.read")
    @Operation(
        summary = "Gerar OpenAPI spec para versão padrão",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<String> generateOpenApi(
            @PathVariable UUID apiId,
            @RequestParam(defaultValue = "json") String format) {
        
        String spec = generatorService.generateOpenApiSpec(apiId, format, false);
        
        MediaType mediaType = "yaml".equalsIgnoreCase(format) 
            ? MediaType.valueOf("application/x-yaml")
            : MediaType.APPLICATION_JSON;
        
        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(spec);
    }
    
    /**
     * Gera spec OpenAPI pública por slug (sem autenticação)
     * Para APIs públicas, qualquer um pode acessar
     * Para APIs privadas/pagas, apenas quem tem subscription ativa
     */
    @GetMapping("/public/docs/{slug}/openapi")
    @Operation(summary = "Gerar OpenAPI spec pública")
    public ResponseEntity<String> generatePublicOpenApi(
            @PathVariable String slug,
            @RequestParam(defaultValue = "json") String format,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String spec = generatorService.generateOpenApiSpecBySlug(slug, format, authHeader, true);
        
        MediaType mediaType = "yaml".equalsIgnoreCase(format) 
            ? MediaType.valueOf("application/x-yaml")
            : MediaType.APPLICATION_JSON;
        
        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(spec);
    }
    
    /**
     * Gera spec OpenAPI para consumer autenticado
     * Verifica se tem subscription ativa
     */
    @GetMapping("/api/v1/consumer/docs/{slug}/openapi")
    @Operation(
        summary = "Gerar OpenAPI spec para consumer",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<String> generateConsumerOpenApi(
            @PathVariable String slug,
            @RequestParam(defaultValue = "json") String format,
            Authentication authentication) {
        
        String consumerId = getUserId(authentication);
        String spec = generatorService.generateOpenApiSpecForConsumer(slug, format, consumerId);
        
        MediaType mediaType = "yaml".equalsIgnoreCase(format) 
            ? MediaType.valueOf("application/x-yaml")
            : MediaType.APPLICATION_JSON;
        
        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(spec);
    }
    
    private String getUserId(Authentication authentication) {
        org.springframework.security.oauth2.jwt.Jwt jwt = 
            (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
