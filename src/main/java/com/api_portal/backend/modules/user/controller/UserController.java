package com.api_portal.backend.modules.user.controller;

import com.api_portal.backend.modules.user.domain.Role;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.dto.UpdateUserRequest;
import com.api_portal.backend.modules.user.dto.UserResponse;
import com.api_portal.backend.modules.user.repository.RoleRepository;
import com.api_portal.backend.modules.user.repository.UserRepository;
import com.api_portal.backend.modules.user.service.KeycloakSyncService;
import com.api_portal.backend.modules.user.service.UserService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gerenciamento de usuários")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {
    
    private final UserService userService;
    private final KeycloakSyncService keycloakSyncService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    
    @GetMapping("/me")
    @Operation(summary = "Obter dados do usuário autenticado")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication,
            HttpServletRequest request) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        String ipAddress = request.getRemoteAddr();
        
        // Sincronizar usuário do Keycloak
        keycloakSyncService.syncUserFromToken(authentication, ipAddress);
        
        UserResponse user = userService.getUserByKeycloakId(keycloakId);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/me")
    @Operation(summary = "Atualizar dados do usuário autenticado")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        UserResponse updated = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }
    
    @GetMapping
    @RequiresPermission("user.read")
    @Operation(summary = "Listar todos os usuários")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/search")
    @RequiresPermission("user.read")
    @Operation(summary = "Buscar usuários")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String query,
            Pageable pageable) {
        
        Page<UserResponse> users = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    @RequiresPermission("user.read")
    @Operation(summary = "Obter usuário por ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("user.update")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        
        UserResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/roles")
    @RequiresPermission("user.manage")
    @Operation(summary = "Atribuir roles ao usuário")
    public ResponseEntity<UserResponse> assignRoles(
            @PathVariable UUID id,
            @RequestBody Set<UUID> roleIds) {
        
        UserResponse updated = userService.assignRoles(id, roleIds);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/deactivate")
    @RequiresPermission("user.manage")
    @Operation(summary = "Desativar usuário")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/activate")
    @RequiresPermission("user.manage")
    @Operation(summary = "Ativar usuário")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/role/{roleCode}")
    @RequiresPermission("user.read")
    @Operation(summary = "Listar usuários por role")
    public ResponseEntity<Page<UserResponse>> getUsersByRole(
            @PathVariable String roleCode,
            Pageable pageable) {
        
        Page<UserResponse> users = userService.getUsersByRole(roleCode, pageable);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/debug/system")
    @Operation(summary = "Debug - Verificar estado do sistema de usuários e roles")
    public ResponseEntity<Map<String, Object>> debugSystem() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            // Contar roles
            long roleCount = roleRepository.count();
            debug.put("totalRoles", roleCount);
            
            // Listar roles
            List<Role> roles = roleRepository.findAll();
            debug.put("roles", roles.stream()
                .map(r -> {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", r.getId());
                    roleMap.put("code", r.getCode());
                    roleMap.put("name", r.getName());
                    roleMap.put("isSystem", r.getIsSystem());
                    roleMap.put("active", r.getActive());
                    return roleMap;
                })
                .collect(Collectors.toList()));
            
            // Contar usuários
            long userCount = userRepository.count();
            debug.put("totalUsers", userCount);
            
            // Listar usuários com suas roles
            List<User> users = userRepository.findAll();
            debug.put("users", users.stream()
                .map(u -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", u.getId());
                    userMap.put("email", u.getEmail());
                    userMap.put("firstName", u.getFirstName());
                    userMap.put("rolesCount", u.getRoles().size());
                    userMap.put("roles", u.getRoles().stream()
                        .map(Role::getCode)
                        .collect(Collectors.toList()));
                    return userMap;
                })
                .collect(Collectors.toList()));
            
            // Contar total de relacionamentos user_roles
            long totalUserRoles = users.stream()
                .mapToLong(u -> u.getRoles().size())
                .sum();
            debug.put("totalUserRolesRelationships", totalUserRoles);
            
            // Status
            debug.put("status", "OK");
            debug.put("message", "Sistema funcionando corretamente");
            
        } catch (Exception e) {
            debug.put("status", "ERROR");
            debug.put("error", e.getMessage());
            debug.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(debug);
    }
}
