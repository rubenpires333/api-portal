package com.api_portal.backend.modules.auth.service;

import com.api_portal.backend.modules.auth.dto.RegisterRequest;
import com.api_portal.backend.modules.auth.exception.AuthException;
import com.api_portal.backend.modules.auth.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {
    
    private final RestTemplate restTemplate;
    
    @Value("${keycloak.url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm:apicv}")
    private String realm;
    
    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;
    
    @Value("${keycloak.admin.password:admin123}")
    private String adminPassword;
    
    public void createUser(RegisterRequest request) {
        try {
            String adminToken = getAdminToken();
            String userUrl = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);
            
            Map<String, Object> userRepresentation = new HashMap<>();
            userRepresentation.put("username", request.getEmail());
            userRepresentation.put("email", request.getEmail());
            userRepresentation.put("firstName", request.getName().split(" ")[0]);
            if (request.getName().split(" ").length > 1) {
                userRepresentation.put("lastName", request.getName().substring(request.getName().indexOf(" ") + 1));
            }
            userRepresentation.put("enabled", true);
            userRepresentation.put("emailVerified", false);
            
            // Credentials
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", request.getPassword());
            credential.put("temporary", false);
            userRepresentation.put("credentials", Collections.singletonList(credential));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userRepresentation, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                userUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                // Extrair ID do usuário criado do header Location
                String location = response.getHeaders().getFirst("Location");
                if (location != null) {
                    String userId = location.substring(location.lastIndexOf("/") + 1);
                    
                    // Atribuir role ao usuário
                    assignRoleToUser(userId, request.getRole(), adminToken);
                    
                    // Adicionar usuário ao grupo
                    addUserToGroup(userId, request.getRole(), adminToken);
                }
                log.info("Utilizador criado com sucesso: {}", request.getEmail());
            } else {
                throw new AuthException("Falha ao criar utilizador no Keycloak");
            }
            
        } catch (Exception e) {
            log.error("Erro ao criar utilizador: {}", e.getMessage());
            throw new AuthException("Email já está em uso ou erro ao criar utilizador", e);
        }
    }
    
    private String getAdminToken() {
        try {
            String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", "admin-cli");
            body.add("username", adminUsername);
            body.add("password", adminPassword);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            
            throw new AuthException("Falha ao obter token de admin");
            
        } catch (Exception e) {
            log.error("Erro ao obter token de admin: {}", e.getMessage());
            throw new AuthException("Erro ao autenticar como admin", e);
        }
    }
    
    private void assignRoleToUser(String userId, UserRole role, String adminToken) {
        try {
            // Mapear role para nome do Keycloak
            String roleName = mapRoleToKeycloak(role);
            
            // Obter role ID
            String rolesUrl = String.format("%s/admin/realms/%s/roles/%s", keycloakUrl, realm, roleName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            ResponseEntity<Map> roleResponse = restTemplate.exchange(
                rolesUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            if (roleResponse.getStatusCode() == HttpStatus.OK && roleResponse.getBody() != null) {
                Map<String, Object> roleData = roleResponse.getBody();
                
                // Atribuir role ao usuário
                String assignRoleUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", 
                    keycloakUrl, realm, userId);
                
                List<Map<String, Object>> roles = Collections.singletonList(roleData);
                
                HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(roles, headers);
                
                restTemplate.exchange(
                    assignRoleUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
                );
                
                log.info("Role {} atribuída ao utilizador {}", roleName, userId);
            }
            
        } catch (Exception e) {
            log.error("Erro ao atribuir role: {}", e.getMessage());
            // Não lançar exceção aqui para não bloquear o registro
        }
    }
    
    private String mapRoleToKeycloak(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> "platform-admin";
            case PROVIDER -> "api-provider";
            case CONSUMER -> "api-consumer";
        };
    }
    
    private void addUserToGroup(String userId, UserRole role, String adminToken) {
        try {
            // Mapear role para nome do grupo
            String groupName = mapRoleToGroupName(role);
            
            // Buscar o ID do grupo pelo nome
            String groupsUrl = String.format("%s/admin/realms/%s/groups?search=%s", 
                keycloakUrl, realm, groupName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            ResponseEntity<List> groupsResponse = restTemplate.exchange(
                groupsUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
            );
            
            if (groupsResponse.getStatusCode() == HttpStatus.OK && 
                groupsResponse.getBody() != null && 
                !groupsResponse.getBody().isEmpty()) {
                
                @SuppressWarnings("unchecked")
                Map<String, Object> group = (Map<String, Object>) groupsResponse.getBody().get(0);
                String groupId = (String) group.get("id");
                
                // Adicionar usuário ao grupo
                String addToGroupUrl = String.format("%s/admin/realms/%s/users/%s/groups/%s", 
                    keycloakUrl, realm, userId, groupId);
                
                restTemplate.exchange(
                    addToGroupUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(headers),
                    String.class
                );
                
                log.info("Utilizador {} adicionado ao grupo {}", userId, groupName);
            } else {
                log.warn("Grupo {} não encontrado no Keycloak. Criando grupo...", groupName);
                createGroupAndAddUser(userId, groupName, adminToken);
            }
            
        } catch (Exception e) {
            log.error("Erro ao adicionar utilizador ao grupo: {}", e.getMessage());
            // Não lançar exceção para não bloquear o registro
        }
    }
    
    private void createGroupAndAddUser(String userId, String groupName, String adminToken) {
        try {
            // Criar grupo
            String groupsUrl = String.format("%s/admin/realms/%s/groups", keycloakUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);
            
            Map<String, Object> groupRepresentation = new HashMap<>();
            groupRepresentation.put("name", groupName);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(groupRepresentation, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                groupsUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("Grupo {} criado com sucesso", groupName);
                // Tentar adicionar o usuário ao grupo novamente
                addUserToGroup(userId, mapGroupNameToRole(groupName), adminToken);
            }
            
        } catch (Exception e) {
            log.error("Erro ao criar grupo: {}", e.getMessage());
        }
    }
    
    private String mapRoleToGroupName(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> "SUPER_ADMIN";
            case PROVIDER -> "PROVIDER";
            case CONSUMER -> "CONSUMER";
        };
    }
    
    private UserRole mapGroupNameToRole(String groupName) {
        return switch (groupName) {
            case "SUPER_ADMIN" -> UserRole.SUPER_ADMIN;
            case "PROVIDER" -> UserRole.PROVIDER;
            case "CONSUMER" -> UserRole.CONSUMER;
            default -> UserRole.CONSUMER;
        };
    }
    
    public void updateUserPassword(String userId, String currentPassword, String newPassword) {
        try {
            String adminToken = getAdminToken();
            
            // Primeiro, validar a senha atual fazendo login
            validateCurrentPassword(userId, currentPassword);
            
            // Atualizar senha
            String resetPasswordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", 
                keycloakUrl, realm, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);
            
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", newPassword);
            credential.put("temporary", false);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(credential, headers);
            
            restTemplate.exchange(
                resetPasswordUrl,
                HttpMethod.PUT,
                entity,
                String.class
            );
            
            log.info("Senha atualizada com sucesso para o utilizador: {}", userId);
            
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao atualizar senha: {}", e.getMessage());
            throw new AuthException("Erro ao atualizar senha", e);
        }
    }
    
    private void validateCurrentPassword(String userId, String currentPassword) {
        try {
            // Obter email do usuário
            String adminToken = getAdminToken();
            String userUrl = String.format("%s/admin/realms/%s/users/%s", keycloakUrl, realm, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            ResponseEntity<Map> userResponse = restTemplate.exchange(
                userUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = userResponse.getBody();
                String username = (String) user.get("username");
                
                // Tentar fazer login com a senha atual
                String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
                
                HttpHeaders tokenHeaders = new HttpHeaders();
                tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("grant_type", "password");
                body.add("client_id", "admin-cli");
                body.add("username", username);
                body.add("password", currentPassword);
                
                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, tokenHeaders);
                
                try {
                    restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);
                } catch (Exception e) {
                    throw new AuthException("Senha atual incorreta");
                }
            }
            
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao validar senha atual: {}", e.getMessage());
            throw new AuthException("Erro ao validar senha atual", e);
        }
    }
    
    public void updateUserInfo(String userId, String name, String email, String phone, String company, String jobTitle) {
        try {
            String adminToken = getAdminToken();
            String userUrl = String.format("%s/admin/realms/%s/users/%s", keycloakUrl, realm, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);
            
            Map<String, Object> userRepresentation = new HashMap<>();
            
            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                userRepresentation.put("firstName", nameParts[0]);
                if (nameParts.length > 1) {
                    userRepresentation.put("lastName", nameParts[1]);
                }
            }
            
            if (email != null && !email.isEmpty()) {
                userRepresentation.put("email", email);
                userRepresentation.put("username", email);
            }
            
            // Atributos customizados
            Map<String, Object> attributes = new HashMap<>();
            if (phone != null && !phone.isEmpty()) {
                attributes.put("phone", Collections.singletonList(phone));
            }
            if (company != null && !company.isEmpty()) {
                attributes.put("company", Collections.singletonList(company));
            }
            if (jobTitle != null && !jobTitle.isEmpty()) {
                attributes.put("jobTitle", Collections.singletonList(jobTitle));
            }
            
            if (!attributes.isEmpty()) {
                userRepresentation.put("attributes", attributes);
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userRepresentation, headers);
            
            restTemplate.exchange(
                userUrl,
                HttpMethod.PUT,
                entity,
                String.class
            );
            
            log.info("Informações atualizadas com sucesso para o utilizador: {}", userId);
            
        } catch (Exception e) {
            log.error("Erro ao atualizar informações do utilizador: {}", e.getMessage());
            throw new AuthException("Erro ao atualizar informações do utilizador", e);
        }
    }
    
    public Map<String, Object> getUserInfo(String userId) {
        try {
            String adminToken = getAdminToken();
            String userUrl = String.format("%s/admin/realms/%s/users/%s", keycloakUrl, realm, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                userUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            throw new AuthException("Utilizador não encontrado");
            
        } catch (Exception e) {
            log.error("Erro ao obter informações do utilizador: {}", e.getMessage());
            throw new AuthException("Erro ao obter informações do utilizador", e);
        }
    }
} 
