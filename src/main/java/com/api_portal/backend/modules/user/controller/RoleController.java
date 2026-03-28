package com.api_portal.backend.modules.user.controller;

import com.api_portal.backend.modules.user.dto.RoleRequest;
import com.api_portal.backend.modules.user.dto.RoleResponse;
import com.api_portal.backend.modules.user.service.RoleService;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Gerenciamento de roles")
@SecurityRequirement(name = "bearer-jwt")
@RequiresPermission("role.read")
public class RoleController {
    
    private final RoleService roleService;
    
    @PostMapping
    @RequiresPermission("role.manage")
    @Operation(summary = "Criar nova role")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }
    
    @GetMapping
    @Operation(summary = "Listar todas as roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter role por ID")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID id) {
        RoleResponse role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }
    
    @GetMapping("/{id}/users")
    @Operation(summary = "Listar usuários de uma role")
    public ResponseEntity<List<RoleResponse.UserInfo>> getRoleUsers(@PathVariable UUID id) {
        List<RoleResponse.UserInfo> users = roleService.getRoleUsers(id);
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("role.manage")
    @Operation(summary = "Atualizar role")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request) {
        
        RoleResponse updated = roleService.updateRole(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("role.manage")
    @Operation(summary = "Deletar role")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
