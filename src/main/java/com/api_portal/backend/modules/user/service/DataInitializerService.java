package com.api_portal.backend.modules.user.service;

import com.api_portal.backend.modules.user.config.PermissionConfig;
import com.api_portal.backend.modules.user.domain.Permission;
import com.api_portal.backend.modules.user.domain.Role;
import com.api_portal.backend.modules.user.repository.PermissionRepository;
import com.api_portal.backend.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializerService implements CommandLineRunner {
    
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionConfig permissionConfig;
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Iniciando verificação de dados do sistema ===");
        
        // Inicializar permissões
        List<Permission> permissions = initializePermissions();
        
        // Inicializar roles
        initializeRoles(permissions);
        
        log.info("=== Verificação de dados concluída ===");
    }
    
    private List<Permission> initializePermissions() {
        log.info("Verificando permissões do sistema...");
        
        List<PermissionData> permissionsData = getDefaultPermissions();
        
        // Adicionar permissões customizadas do application.properties
        if (permissionConfig.getCustom() != null && !permissionConfig.getCustom().isEmpty()) {
            log.info("Carregando {} permissões customizadas da configuração", permissionConfig.getCustom().size());
            for (PermissionConfig.PermissionDefinition custom : permissionConfig.getCustom()) {
                permissionsData.add(new PermissionData(
                    custom.getName(),
                    custom.getCode(),
                    custom.getDescription(),
                    custom.getResource(),
                    custom.getAction()
                ));
            }
        }
        
        List<Permission> createdPermissions = new ArrayList<>();
        int newCount = 0;
        
        for (PermissionData data : permissionsData) {
            if (!permissionRepository.existsByCode(data.code)) {
                Permission permission = Permission.builder()
                    .name(data.name)
                    .code(data.code)
                    .description(data.description)
                    .resource(data.resource)
                    .action(data.action)
                    .active(true)
                    .build();
                
                permission = permissionRepository.save(permission);
                createdPermissions.add(permission);
                newCount++;
                log.info("✓ Permissão criada: {} ({})", permission.getName(), permission.getCode());
            } else {
                Permission existing = permissionRepository.findByCode(data.code).orElse(null);
                if (existing != null) {
                    createdPermissions.add(existing);
                }
            }
        }
        
        if (newCount > 0) {
            log.info("✓ {} novas permissões criadas", newCount);
        }
        log.info("Total de permissões no sistema: {}", permissionRepository.count());
        return createdPermissions;
    }
    
    private void initializeRoles(List<Permission> allPermissions) {
        log.info("Verificando roles do sistema...");
        
        // SUPER_ADMIN
        if (!roleRepository.existsByCode("SUPER_ADMIN")) {
            Set<Permission> superAdminPerms = new HashSet<>(allPermissions);
            
            Role superAdmin = Role.builder()
                .name("Super Admin")
                .code("SUPER_ADMIN")
                .description("Administrador com acesso total ao sistema")
                .isSystem(true)
                .active(true)
                .permissions(superAdminPerms)
                .build();
            
            roleRepository.save(superAdmin);
            log.info("✓ Role criada: SUPER_ADMIN com {} permissões", superAdminPerms.size());
        } else {
            log.info("✓ Role SUPER_ADMIN já existe");
        }
        
        // PROVIDER
        if (!roleRepository.existsByCode("PROVIDER")) {
            Set<Permission> providerPerms = getPermissionsByCode(allPermissions,
                "api.create", "api.read", "api.update", "api.delete", "api.publish",
                "category.read", "user.read", "provider.metrics.read", "wallet.withdraw"
            );
            
            Role provider = Role.builder()
                .name("Provider")
                .code("PROVIDER")
                .description("Provedor de APIs")
                .isSystem(true)
                .active(true)
                .permissions(providerPerms)
                .build();
            
            roleRepository.save(provider);
            log.info("✓ Role criada: PROVIDER com {} permissões", providerPerms.size());
        } else {
            log.info("✓ Role PROVIDER já existe");
        }
        
        // CONSUMER
        if (!roleRepository.existsByCode("CONSUMER")) {
            Set<Permission> consumerPerms = getPermissionsByCode(allPermissions,
                "api.read", "category.read", "user.read", "consumer.metrics.read"
            );
            
            Role consumer = Role.builder()
                .name("Consumer")
                .code("CONSUMER")
                .description("Consumidor de APIs")
                .isSystem(true)
                .active(true)
                .permissions(consumerPerms)
                .build();
            
            roleRepository.save(consumer);
            log.info("✓ Role criada: CONSUMER com {} permissões", consumerPerms.size());
        } else {
            log.info("✓ Role CONSUMER já existe");
        }
        
        log.info("Total de roles no sistema: {}", roleRepository.count());
    }
    
    private Set<Permission> getPermissionsByCode(List<Permission> allPermissions, String... codes) {
        Set<Permission> permissions = new HashSet<>();
        for (String code : codes) {
            allPermissions.stream()
                .filter(p -> p.getCode().equals(code))
                .findFirst()
                .ifPresent(permissions::add);
        }
        return permissions;
    }
    
    private List<PermissionData> getDefaultPermissions() {
        List<PermissionData> permissions = new ArrayList<>();
        
        // Permissões de API
        permissions.add(new PermissionData(
            "Criar API", "api.create", 
            "Permite criar novas APIs", "api", "create"
        ));
        permissions.add(new PermissionData(
            "Ler API", "api.read", 
            "Permite visualizar APIs", "api", "read"
        ));
        permissions.add(new PermissionData(
            "Atualizar API", "api.update", 
            "Permite atualizar APIs", "api", "update"
        ));
        permissions.add(new PermissionData(
            "Deletar API", "api.delete", 
            "Permite deletar APIs", "api", "delete"
        ));
        permissions.add(new PermissionData(
            "Publicar API", "api.publish", 
            "Permite publicar APIs", "api", "publish"
        ));
        
        // Permissões de Categoria
        permissions.add(new PermissionData(
            "Criar Categoria", "category.create", 
            "Permite criar categorias", "category", "create"
        ));
        permissions.add(new PermissionData(
            "Ler Categoria", "category.read", 
            "Permite visualizar categorias", "category", "read"
        ));
        permissions.add(new PermissionData(
            "Atualizar Categoria", "category.update", 
            "Permite atualizar categorias", "category", "update"
        ));
        permissions.add(new PermissionData(
            "Deletar Categoria", "category.delete", 
            "Permite deletar categorias", "category", "delete"
        ));
        
        // Permissões de Usuário
        permissions.add(new PermissionData(
            "Gerenciar Usuários", "user.manage", 
            "Permite gerenciar usuários", "user", "manage"
        ));
        permissions.add(new PermissionData(
            "Ler Usuário", "user.read", 
            "Permite visualizar usuários", "user", "read"
        ));
        permissions.add(new PermissionData(
            "Atualizar Usuário", "user.update", 
            "Permite atualizar usuários", "user", "update"
        ));
        
        // Permissões de Role
        permissions.add(new PermissionData(
            "Gerenciar Roles", "role.manage", 
            "Permite gerenciar roles", "role", "manage"
        ));
        permissions.add(new PermissionData(
            "Ler Role", "role.read", 
            "Permite visualizar roles", "role", "read"
        ));
        
        // Permissões de Permissão
        permissions.add(new PermissionData(
            "Gerenciar Permissões", "permission.manage", 
            "Permite gerenciar permissões", "permission", "manage"
        ));
        permissions.add(new PermissionData(
            "Ler Permissão", "permission.read", 
            "Permite visualizar permissões", "permission", "read"
        ));
        
        // Permissões de Auditoria
        permissions.add(new PermissionData(
            "Ler Auditoria", "audit.read", 
            "Permite visualizar logs de auditoria", "audit", "read"
        ));
        
        // Permissões de Versão de API
        permissions.add(new PermissionData(
            "Criar Versão", "version.create", 
            "Permite criar versões de API", "version", "create"
        ));
        permissions.add(new PermissionData(
            "Ler Versão", "version.read", 
            "Permite visualizar versões de API", "version", "read"
        ));
        permissions.add(new PermissionData(
            "Atualizar Versão", "version.update", 
            "Permite atualizar versões de API", "version", "update"
        ));
        permissions.add(new PermissionData(
            "Deletar Versão", "version.delete", 
            "Permite deletar versões de API", "version", "delete"
        ));
        
        // Permissões de Endpoint
        permissions.add(new PermissionData(
            "Criar Endpoint", "endpoint.create", 
            "Permite criar endpoints", "endpoint", "create"
        ));
        permissions.add(new PermissionData(
            "Ler Endpoint", "endpoint.read", 
            "Permite visualizar endpoints", "endpoint", "read"
        ));
        permissions.add(new PermissionData(
            "Atualizar Endpoint", "endpoint.update", 
            "Permite atualizar endpoints", "endpoint", "update"
        ));
        permissions.add(new PermissionData(
            "Deletar Endpoint", "endpoint.delete", 
            "Permite deletar endpoints", "endpoint", "delete"
        ));
        
        // Permissões de Métricas do Provider
        permissions.add(new PermissionData(
            "Ler Métricas do Provider", "provider.metrics.read", 
            "Permite visualizar métricas das APIs do provider", "provider", "metrics.read"
        ));
        
        // Permissões de Métricas do Consumer
        permissions.add(new PermissionData(
            "Ler Métricas do Consumer", "consumer.metrics.read", 
            "Permite visualizar métricas de uso das APIs", "consumer", "metrics.read"
        ));
        
        // Permissões de Billing
        permissions.add(new PermissionData(
            "Gerenciar Billing", "billing.manage", 
            "Permite gerenciar gateways de pagamento, planos e regras de taxas", "billing", "manage"
        ));
        permissions.add(new PermissionData(
            "Sacar Fundos", "wallet.withdraw", 
            "Permite solicitar saques da carteira", "wallet", "withdraw"
        ));
        
        return permissions;
    }
    
    // Classe interna para dados de permissão
    private static class PermissionData {
        String name;
        String code;
        String description;
        String resource;
        String action;
        
        PermissionData(String name, String code, String description, String resource, String action) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.resource = resource;
            this.action = action;
        }
    }
}
