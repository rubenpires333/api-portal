package com.api_portal.backend.modules.user.controller;

import com.api_portal.backend.modules.user.dto.RoleRequest;
import com.api_portal.backend.modules.user.dto.RoleResponse;
import com.api_portal.backend.modules.user.service.RoleService;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Gerenciamento de roles")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RoleController {
    
    private final RoleService roleService;
    
    @PostMapping
    @Operation(summary = "Criar nova role (SUPER_ADMIN)")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }
    
    @GetMapping
    @Operation(summary = "Listar todas as roles (SUPER_ADMIN)")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter role por ID (SUPER_ADMIN)")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID id) {
        RoleResponse role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar role (SUPER_ADMIN)")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request) {
        
        RoleResponse updated = roleService.updateRole(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar role (SUPER_ADMIN)")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
