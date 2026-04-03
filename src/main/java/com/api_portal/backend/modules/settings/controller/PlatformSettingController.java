package com.api_portal.backend.modules.settings.controller;

import com.api_portal.backend.modules.settings.dto.PlatformSettingDTO;
import com.api_portal.backend.modules.settings.dto.UpdatePlatformSettingRequest;
import com.api_portal.backend.modules.settings.service.PlatformSettingService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings/platform")
@RequiredArgsConstructor
@Tag(name = "Platform Settings", description = "Gerenciamento de configurações da plataforma")
@SecurityRequirement(name = "bearer-jwt")
public class PlatformSettingController {
    
    private final PlatformSettingService service;
    
    @GetMapping
    @RequiresPermission("settings.manage")
    @Operation(summary = "Obter todas as configurações", description = "Lista todas as configurações da plataforma (apenas SUPER_ADMIN)")
    public ResponseEntity<List<PlatformSettingDTO>> getAllSettings() {
        return ResponseEntity.ok(service.getAllSettings());
    }
    
    @GetMapping("/category/{category}")
    @RequiresPermission("settings.manage")
    @Operation(summary = "Obter configurações por categoria", description = "Lista configurações de uma categoria específica")
    public ResponseEntity<List<PlatformSettingDTO>> getSettingsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(service.getSettingsByCategory(category));
    }
    
    @GetMapping("/public")
    @Operation(summary = "Obter configurações públicas", description = "Lista configurações públicas (sem autenticação)")
    public ResponseEntity<List<PlatformSettingDTO>> getPublicSettings() {
        return ResponseEntity.ok(service.getPublicSettings());
    }
    
    @PutMapping
    @RequiresPermission("settings.manage")
    @Operation(summary = "Atualizar configuração", description = "Atualiza o valor de uma configuração")
    public ResponseEntity<PlatformSettingDTO> updateSetting(
            @Valid @RequestBody UpdatePlatformSettingRequest request,
            Authentication authentication) {
        
        String userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userId = jwt.getSubject();
        }
        
        return ResponseEntity.ok(service.updateSetting(request, userId));
    }
}
