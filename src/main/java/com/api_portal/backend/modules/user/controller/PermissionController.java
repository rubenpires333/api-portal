package com.api_portal.backend.modules.user.controller;

import com.api_portal.backend.modules.user.dto.PermissionRequest;
import com.api_portal.backend.modules.user.dto.PermissionResponse;
import com.api_portal.backend.modules.user.service.PermissionManagementService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Gerenciamento de permissões")
@SecurityRequirement(name = "bearer-jwt")
@RequiresPermission("permission.read")
public class PermissionController {
    
    private final PermissionManagementService permissionManagementService;
    
    @PostMapping
    @RequiresPermission("permission.manage")
    @Operation(summary = "Criar nova permissão")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        PermissionResponse permission = permissionManagementService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(permission);
    }
    
    @GetMapping
    @Operation(summary = "Listar todas as permissões")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> permissions = permissionManagementService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }
    
    @GetMapping("/resource/{resource}")
    @Operation(summary = "Listar permissões por recurso")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByResource(@PathVariable String resource) {
        List<PermissionResponse> permissions = permissionManagementService.getPermissionsByResource(resource);
        return ResponseEntity.ok(permissions);
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("permission.manage")
    @Operation(summary = "Deletar permissão")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        permissionManagementService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
