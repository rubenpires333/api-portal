package com.api_portal.backend.modules.user.service;

import com.api_portal.backend.modules.user.domain.Role;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.dto.UpdateUserRequest;
import com.api_portal.backend.modules.user.dto.UserResponse;
import com.api_portal.backend.modules.user.repository.RoleRepository;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User createOrUpdateUser(String keycloakId, String email, String firstName, 
                                   String lastName, String username, Boolean emailVerified, 
                                   List<String> roleCodes) {
        log.info("=== Iniciando createOrUpdateUser para: {} ===", email);
        log.info("Roles do JWT: {}", roleCodes);
        
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElse(null);
        
        boolean isNewUser = (user == null);
        log.info("Usuário é novo? {}", isNewUser);
        
        if (isNewUser) {
            log.info("Criando novo usuário...");
            
            // Criar novo usuário
            user = User.builder()
                .keycloakId(keycloakId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .emailVerified(emailVerified != null ? emailVerified : false)
                .active(true)
                .roles(new HashSet<>())
                .build();
            
            log.info("Usuário criado em memória: {}", user.getEmail());
        } else {
            log.info("Atualizando usuário existente: {}", email);
            
            // Atualizar informações
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            if (emailVerified != null) {
                user.setEmailVerified(emailVerified);
            }
        }
        
        // Sincronizar roles do JWT com o banco de dados
        if (roleCodes != null && !roleCodes.isEmpty()) {
            log.info("Sincronizando {} roles do JWT...", roleCodes.size());
            
            Set<Role> rolesToAssign = new HashSet<>();
            
            for (String roleCode : roleCodes) {
                // Ignorar roles do Keycloak que não são do nosso sistema
                if (roleCode.startsWith("default-") || roleCode.startsWith("offline_") || 
                    roleCode.equals("uma_authorization")) {
                    log.debug("Ignorando role do Keycloak: {}", roleCode);
                    continue;
                }
                
                Role role = roleRepository.findByCode(roleCode.toUpperCase())
                    .orElse(null);
                
                if (role != null) {
                    rolesToAssign.add(role);
                    log.info("✓ Role encontrada: {} ({})", role.getName(), role.getCode());
                } else {
                    log.warn("⚠ Role '{}' do JWT não encontrada no banco de dados", roleCode);
                }
            }
            
            if (!rolesToAssign.isEmpty()) {
                user.setRoles(rolesToAssign);
                log.info("✓ {} roles atribuídas ao usuário", rolesToAssign.size());
            } else {
                log.warn("⚠ Nenhuma role válida encontrada. Atribuindo CONSUMER por padrão...");
                
                // Fallback: atribuir CONSUMER se nenhuma role válida foi encontrada
                Role consumerRole = roleRepository.findByCode("CONSUMER").orElse(null);
                if (consumerRole != null) {
                    user.getRoles().add(consumerRole);
                    log.info("✓ Role CONSUMER atribuída como fallback");
                }
            }
        } else {
            log.warn("⚠ Nenhuma role no JWT. Atribuindo CONSUMER por padrão...");
            
            // Se não há roles no JWT, atribuir CONSUMER
            if (user.getRoles().isEmpty()) {
                Role consumerRole = roleRepository.findByCode("CONSUMER").orElse(null);
                if (consumerRole != null) {
                    user.getRoles().add(consumerRole);
                    log.info("✓ Role CONSUMER atribuída (sem roles no JWT)");
                }
            }
        }
        
        // Salvar usuário
        user = userRepository.save(user);
        log.info("✓ Usuário salvo no banco com ID: {}", user.getId());
        log.info("Total de roles no usuário após salvar: {}", user.getRoles().size());
        
        // Listar roles atribuídas
        if (!user.getRoles().isEmpty()) {
            user.getRoles().forEach(r -> 
                log.info("  - Role: {} ({})", r.getName(), r.getCode())
            );
            log.info("✓✓✓ SUCESSO: Roles sincronizadas para usuário: {}", email);
        } else {
            log.error("❌ ERRO: Usuário salvo mas sem roles!");
        }
        
        log.info("=== Finalizando createOrUpdateUser ===");
        return user;
    }
    
    @Transactional
    public void updateLastLogin(UUID id, String ipAddress) {
        log.info("Atualizando último login do usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        return mapToResponse(user);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'keycloak:' + #keycloakId")
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        return mapToResponse(user);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(String roleCode, Pageable pageable) {
        return userRepository.findByRoleCode(roleCode, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Atualizando usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getCompany() != null) {
            user.setCompany(request.getCompany());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            user.setWebsite(request.getWebsite());
        }
        
        user = userRepository.save(user);
        
        return mapToResponse(user);
    }
    
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse assignRoles(UUID userId, Set<UUID> roleIds) {
        log.info("Atribuindo roles ao usuário: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Set<Role> roles = new HashSet<>();
        for (UUID roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role não encontrada: " + roleId));
            roles.add(role);
        }
        
        user.setRoles(roles);
        user = userRepository.save(user);
        
        return mapToResponse(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deactivateUser(UUID id) {
        log.info("Desativando usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        user.setActive(false);
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void activateUser(UUID id) {
        log.info("Ativando usuário: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        user.setActive(true);
        userRepository.save(user);
    }
    
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .keycloakId(user.getKeycloakId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFullName())
            .username(user.getUsername())
            .phoneNumber(user.getPhoneNumber())
            .avatarUrl(user.getAvatarUrl())
            .emailVerified(user.getEmailVerified())
            .active(user.getActive())
            .lastLoginAt(user.getLastLoginAt())
            .lastLoginIp(user.getLastLoginIp())
            .roles(user.getRoles().stream()
                .map(role -> UserResponse.RoleInfo.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .code(role.getCode())
                    .description(role.getDescription())
                    .build())
                .collect(Collectors.toSet()))
            .bio(user.getBio())
            .company(user.getCompany())
            .location(user.getLocation())
            .website(user.getWebsite())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .createdBy(user.getCreatedBy())
            .lastModifiedBy(user.getLastModifiedBy())
            .build();
    }
}
