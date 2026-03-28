package com.api_portal.backend.modules.user.service;

import com.api_portal.backend.modules.user.domain.Permission;
import com.api_portal.backend.modules.user.dto.PermissionRequest;
import com.api_portal.backend.modules.user.dto.PermissionResponse;
import com.api_portal.backend.modules.user.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final PermissionRepository permissionRepository;
    
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionResponse createPermission(PermissionRequest request) {
        log.info("Criando permissão: {}", request.getName());
        
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Permissão com este código já existe");
        }
        
        Permission permission = Permission.builder()
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .resource(request.getResource())
            .action(request.getAction())
            .active(true)
            .build();
        
        permission = permissionRepository.save(permission);
        
        return mapToResponse(permission);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "permissions", key = "'all'")
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllActive()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public void deletePermission(UUID id) {
        log.info("Deletando permissão: {}", id);
        
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Permissão não encontrada"));
        
        if (!permission.getRoles().isEmpty()) {
            throw new RuntimeException("Não é possível deletar permissão associada a roles");
        }
        
        permissionRepository.delete(permission);
    }
    
    private PermissionResponse mapToResponse(Permission permission) {
        return PermissionResponse.builder()
            .id(permission.getId())
            .name(permission.getName())
            .code(permission.getCode())
            .description(permission.getDescription())
            .resource(permission.getResource())
            .action(permission.getAction())
            .active(permission.getActive())
            .createdAt(permission.getCreatedAt())
            .updatedAt(permission.getUpdatedAt())
            .build();
    }
}
