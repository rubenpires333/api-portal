package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.dto.swagger.ImportSwaggerRequest;
import com.api_portal.backend.modules.api.dto.swagger.ImportSwaggerResponse;
import com.api_portal.backend.modules.api.dto.swagger.SwaggerPreviewResponse;
import com.api_portal.backend.modules.api.service.SwaggerImportService;
import com.api_portal.backend.shared.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/apis/import")
@RequiredArgsConstructor
public class SwaggerImportController {
    
    private final SwaggerImportService importService;
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int URL_TIMEOUT = 10000; // 10 segundos
    
    /**
     * Preview de Swagger (upload ou URL)
     */
    @PostMapping("/preview")
    @RequiresPermission("api.create")
    public ResponseEntity<SwaggerPreviewResponse> previewSwagger(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String url) {
        
        try {
            String content;
            
            if (file != null) {
                // Validar tamanho
                if (file.getSize() > MAX_FILE_SIZE) {
                    return ResponseEntity.badRequest()
                        .body(SwaggerPreviewResponse.builder()
                            .warnings(List.of("Arquivo muito grande. Máximo: 5MB"))
                            .build());
                }
                
                // Validar tipo
                String contentType = file.getContentType();
                if (contentType != null && 
                    !contentType.contains("json") && 
                    !contentType.contains("yaml") && 
                    !contentType.contains("yml")) {
                    return ResponseEntity.badRequest()
                        .body(SwaggerPreviewResponse.builder()
                            .warnings(List.of("Tipo de arquivo inválido. Use JSON ou YAML"))
                            .build());
                }
                
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
                log.info("Preview de Swagger via upload: {} bytes", content.length());
                
            } else if (url != null && !url.isEmpty()) {
                content = fetchFromUrl(url);
                log.info("Preview de Swagger via URL: {}", url);
                
            } else {
                return ResponseEntity.badRequest().build();
            }
            
            SwaggerPreviewResponse preview = importService.parseSwagger(content);
            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            log.error("Erro ao gerar preview: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(SwaggerPreviewResponse.builder()
                    .warnings(List.of("Erro ao processar Swagger: " + e.getMessage()))
                    .build());
        }
    }
    
    /**
     * Executar importação
     */
    @PostMapping("/execute")
    @RequiresPermission("api.create")
    public ResponseEntity<ImportSwaggerResponse> executeImport(
            @RequestBody ImportSwaggerRequest request,
            Authentication authentication) {
        
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String providerId = jwt.getSubject();
            String providerName = jwt.getClaimAsString("name");
            String providerEmail = jwt.getClaimAsString("email");
            
            if (providerName == null || providerName.isEmpty()) {
                providerName = jwt.getClaimAsString("preferred_username");
            }
            
            ImportSwaggerResponse response = importService.importSwagger(
                request,
                providerId,
                providerName,
                providerEmail
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao importar Swagger: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Busca conteúdo de URL remota
     */
    private String fetchFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(URL_TIMEOUT);
            connection.setReadTimeout(URL_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json, application/yaml, text/yaml");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Erro ao buscar URL: HTTP " + responseCode);
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
            
        } finally {
            connection.disconnect();
        }
    }
}
