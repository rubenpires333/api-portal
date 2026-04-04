package com.api_portal.backend.modules.consumer.controller;

import com.api_portal.backend.modules.api.dto.ApiResponse;
import com.api_portal.backend.modules.consumer.service.ConsumerApiService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/consumer/my-apis")
@RequiredArgsConstructor
@Tag(name = "Consumer My APIs", description = "Minhas APIs com subscrição ativa")
@SecurityRequirement(name = "Bearer Authentication")
public class ConsumerMyApisController {
    
    private final ConsumerApiService consumerApiService;
    
    @GetMapping
    @RequiresPermission("consumer.apis.read")
    @Operation(summary = "Listar minhas APIs", description = "Retorna todas as APIs com subscrição ativa do consumer")
    public ResponseEntity<Page<ApiResponse>> getMyApis(
            @PageableDefault(size = 12) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        
        UUID consumerId = UUID.fromString(authentication.getName());
        log.info("Buscando APIs com subscrição ativa para consumer: {}", consumerId);
        
        Page<ApiResponse> apis = consumerApiService.getMyApis(consumerId, search, category, pageable);
        return ResponseEntity.ok(apis);
    }
}
