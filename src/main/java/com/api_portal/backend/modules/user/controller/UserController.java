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
    
    @PutMapping("/me/profile")
    @Operation(summary = "Atualizar perfil completo do usuário autenticado")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody com.api_portal.backend.modules.user.dto.UpdateProfileRequest request,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        UserResponse updated = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }
    
    @GetMapping("/me/addresses")
    @Operation(summary = "Listar endereços do usuário autenticado")
    public ResponseEntity<List<com.api_portal.backend.modules.user.dto.AddressResponse>> getMyAddresses(
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        List<com.api_portal.backend.modules.user.dto.AddressResponse> addresses = 
            userService.getUserAddresses(currentUser.getId());
        return ResponseEntity.ok(addresses);
    }
    
    @PostMapping("/me/addresses")
    @Operation(summary = "Adicionar endereço ao usuário autenticado")
    public ResponseEntity<com.api_portal.backend.modules.user.dto.AddressResponse> addAddress(
            @Valid @RequestBody com.api_portal.backend.modules.user.dto.AddressRequest request,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        com.api_portal.backend.modules.user.dto.AddressResponse address = 
            userService.addAddress(currentUser.getId(), request);
        return ResponseEntity.ok(address);
    }
    
    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Atualizar endereço do usuário autenticado")
    public ResponseEntity<com.api_portal.backend.modules.user.dto.AddressResponse> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody com.api_portal.backend.modules.user.dto.AddressRequest request,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        com.api_portal.backend.modules.user.dto.AddressResponse address = 
            userService.updateAddress(currentUser.getId(), addressId, request);
        return ResponseEntity.ok(address);
    }
    
    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Remover endereço do usuário autenticado")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID addressId,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        userService.deleteAddress(currentUser.getId(), addressId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/me/contacts")
    @Operation(summary = "Listar contatos do usuário autenticado")
    public ResponseEntity<List<com.api_portal.backend.modules.user.dto.ContactResponse>> getMyContacts(
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        List<com.api_portal.backend.modules.user.dto.ContactResponse> contacts = 
            userService.getUserContacts(currentUser.getId());
        return ResponseEntity.ok(contacts);
    }
    
    @PostMapping("/me/contacts")
    @Operation(summary = "Adicionar contato ao usuário autenticado")
    public ResponseEntity<com.api_portal.backend.modules.user.dto.ContactResponse> addContact(
            @Valid @RequestBody com.api_portal.backend.modules.user.dto.ContactRequest request,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        com.api_portal.backend.modules.user.dto.ContactResponse contact = 
            userService.addContact(currentUser.getId(), request);
        return ResponseEntity.ok(contact);
    }
    
    @PutMapping("/me/contacts/{contactId}")
    @Operation(summary = "Atualizar contato do usuário autenticado")
    public ResponseEntity<com.api_portal.backend.modules.user.dto.ContactResponse> updateContact(
            @PathVariable UUID contactId,
            @Valid @RequestBody com.api_portal.backend.modules.user.dto.ContactRequest request,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        com.api_portal.backend.modules.user.dto.ContactResponse contact = 
            userService.updateContact(currentUser.getId(), contactId, request);
        return ResponseEntity.ok(contact);
    }
    
    @DeleteMapping("/me/contacts/{contactId}")
    @Operation(summary = "Remover contato do usuário autenticado")
    public ResponseEntity<Void> deleteContact(
            @PathVariable UUID contactId,
            Authentication authentication) {
        
        String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
        UserResponse currentUser = userService.getUserByKeycloakId(keycloakId);
        
        userService.deleteContact(currentUser.getId(), contactId);
        return ResponseEntity.noContent().build();
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
    
    @PostMapping("/debug/assign-super-admin")
    @Operation(summary = "Debug - Atribuir role SUPER_ADMIN a todos os usuários (TEMPORÁRIO)")
    public ResponseEntity<Map<String, Object>> assignSuperAdminToAll() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Role superAdminRole = roleRepository.findByCode("SUPER_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role SUPER_ADMIN não encontrada"));
            
            List<User> users = userRepository.findAll();
            int assigned = 0;
            
            for (User user : users) {
                if (!user.getRoles().contains(superAdminRole)) {
                    user.getRoles().add(superAdminRole);
                    userRepository.save(user);
                    assigned++;
                }
            }
            
            result.put("status", "SUCCESS");
            result.put("message", "Role SUPER_ADMIN atribuída com sucesso");
            result.put("totalUsers", users.size());
            result.put("usersAssigned", assigned);
            result.put("usersAlreadyHadRole", users.size() - assigned);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/me/force-sync")
    @Operation(summary = "Forçar sincronização do usuário atual com Keycloak")
    public ResponseEntity<Map<String, Object>> forceSyncCurrentUser(
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String keycloakId = keycloakSyncService.extractKeycloakId(authentication);
            String ipAddress = request.getRemoteAddr();
            
            // Adicionar informações do JWT para debug
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
                org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
                result.put("jwtClaims", jwt.getClaims().keySet());
                result.put("jwtSubject", jwt.getSubject());
                result.put("jwtEmail", jwt.getClaimAsString("email"));
                
                // Tentar extrair realm_access
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                if (realmAccess != null) {
                    result.put("realmAccess", realmAccess);
                }
                
                // Tentar extrair resource_access
                Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                if (resourceAccess != null) {
                    result.put("resourceAccess", resourceAccess);
                }
            }
            
            // Sincronizar usuário do Keycloak
            User user = keycloakSyncService.syncUserFromToken(authentication, ipAddress);
            
            if (user != null) {
                result.put("status", "SUCCESS");
                result.put("message", "Usuário sincronizado com sucesso");
                result.put("userId", user.getId());
                result.put("email", user.getEmail());
                result.put("rolesCount", user.getRoles().size());
                result.put("roles", user.getRoles().stream()
                    .map(Role::getCode)
                    .collect(Collectors.toList()));
            } else {
                result.put("status", "ERROR");
                result.put("message", "Falha ao sincronizar usuário");
            }
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}