package com.api_portal.backend.modules.user.controller;

import com.api_portal.backend.modules.user.dto.PermissionRequest;
import com.api_portal.backend.modules.user.dto.PermissionResponse;
import com.api_portal.backend.modules.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Gerenciamento de permissões")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PermissionController {
    
    private final PermissionService permissionService;
    
    @PostMapping
    @Operation(summary = "Criar nova permissão (SUPER_ADMIN)")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        PermissionResponse permission = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(permission);
    }
    
    @GetMapping
    @Operation(summary = "Listar todas as permissões (SUPER_ADMIN)")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }
    
    @GetMapping("/resource/{resource}")
    @Operation(summary = "Listar permissões por recurso (SUPER_ADMIN)")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByResource(@PathVariable String resource) {
        List<PermissionResponse> permissions = permissionService.getPermissionsByResource(resource);
        return ResponseEntity.ok(permissions);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar permissão (SUPER_ADMIN)")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
