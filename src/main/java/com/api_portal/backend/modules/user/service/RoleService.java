package com.api_portal.backend.modules.user.service;

import com.api_portal.backend.modules.user.domain.Permission;
import com.api_portal.backend.modules.user.domain.Role;
import com.api_portal.backend.modules.user.dto.RoleRequest;
import com.api_portal.backend.modules.user.dto.RoleResponse;
import com.api_portal.backend.modules.user.repository.PermissionRepository;
import com.api_portal.backend.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RoleResponse createRole(RoleRequest request) {
        log.info("Criando role: {}", request.getName());
        
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Role com este código já existe");
        }
        
        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null) {
            for (UUID permId : request.getPermissionIds()) {
                Permission permission = permissionRepository.findById(permId)
                    .orElseThrow(() -> new RuntimeException("Permissão não encontrada: " + permId));
                permissions.add(permission);
            }
        }
        
        Role role = Role.builder()
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .isSystem(false)
            .active(true)
            .permissions(permissions)
            .build();
        
        role = roleRepository.save(role);
        
        return mapToResponse(role);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "'all'")
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#id")
    public RoleResponse getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role não encontrada"));
        
        return mapToResponse(role);
    }
    
    @Transactional
    @CachePut(value = "roles", key = "#id")
    @CacheEvict(value = "roles", key = "'all'")
    public RoleResponse updateRole(UUID id, RoleRequest request) {
        log.info("Atualizando role: {}", id);
        
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role não encontrada"));
        
        if (role.getIsSystem()) {
            throw new RuntimeException("Não é possível atualizar role do sistema");
        }
        
        if (!role.getCode().equals(request.getCode()) && roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Role com este código já existe");
        }
        
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        
        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (UUID permId : request.getPermissionIds()) {
                Permission permission = permissionRepository.findById(permId)
                    .orElseThrow(() -> new RuntimeException("Permissão não encontrada: " + permId));
                permissions.add(permission);
            }
            role.setPermissions(permissions);
        }
        
        role = roleRepository.save(role);
        
        return mapToResponse(role);
    }
    
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public void deleteRole(UUID id) {
        log.info("Deletando role: {}", id);
        
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role não encontrada"));
        
        if (role.getIsSystem()) {
            throw new RuntimeException("Não é possível deletar role do sistema");
        }
        
        if (!role.getUsers().isEmpty()) {
            throw new RuntimeException("Não é possível deletar role com usuários associados");
        }
        
        roleRepository.delete(role);
    }
    
    private RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
            .id(role.getId())
            .name(role.getName())
            .code(role.getCode())
            .description(role.getDescription())
            .isSystem(role.getIsSystem())
            .active(role.getActive())
            .permissions(role.getPermissions().stream()
                .map(perm -> RoleResponse.PermissionInfo.builder()
                    .id(perm.getId())
                    .name(perm.getName())
                    .code(perm.getCode())
                    .resource(perm.getResource())
                    .action(perm.getAction())
                    .build())
                .collect(Collectors.toSet()))
            .userCount(role.getUsers().size())
            .createdAt(role.getCreatedAt())
            .updatedAt(role.getUpdatedAt())
            .build();
    }
}
