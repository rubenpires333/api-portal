package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.dto.ApiPublicResponse;
import com.api_portal.backend.modules.api.service.ApiService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consumer/apis")
@RequiredArgsConstructor
@Tag(name = "Consumer APIs", description = "APIs para consumidores")
@SecurityRequirement(name = "bearer-jwt")
public class ConsumerApiController {
    
    private final ApiService apiService;
    
    @GetMapping("/public")
    @Operation(summary = "Listar APIs publicas disponiveis no marketplace")
    public ResponseEntity<Page<ApiPublicResponse>> getPublicApis(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        String consumerId = getUserId(authentication);
        return ResponseEntity.ok(apiService.exploreApis(search, categoryId, consumerId, pageable));
    }
    
    @GetMapping("/public/{id}")
    @Operation(summary = "Detalhes de uma API publica")
    public ResponseEntity<ApiPublicResponse> getPublicApiById(
            @PathVariable UUID id,
            Authentication authentication) {
        String consumerId = getUserId(authentication);
        return ResponseEntity.ok(apiService.getApiDetailsForConsumer(id, consumerId));
    }
    
    private String getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }
}
