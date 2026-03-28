package com.api_portal.backend.modules.auth.service;

import com.api_portal.backend.modules.auth.dto.*;
import com.api_portal.backend.modules.auth.exception.AuthException;
import com.api_portal.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final RestTemplate restTemplate;
    private final KeycloakAdminService keycloakAdminService;
    private final JwtDecoder jwtDecoder;
    private final UserService userService;
    
    @Value("${keycloak.url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm:apicv}")
    private String realm;
    
    @Value("${keycloak.client-id:apicv-backend}")
    private String clientId;
    
    @Value("${keycloak.client-secret:change-me-in-production}")
    private String clientSecret;
    
    public TokenResponse login(LoginRequest request, String ipAddress) {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", request.getEmail());
            body.add("password", request.getPassword());
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");
                
                // Decodificar o token e sincronizar usuário
                TokenResponse.UserInfo userInfo = null;
                Jwt jwt = jwtDecoder.decode(accessToken);
                
                // Sincronizar e validar usuário (lança exceção se inativo)
                syncUserFromJwt(jwt, ipAddress);
                
                // Extrair informações do usuário do JWT
                userInfo = TokenResponse.UserInfo.builder()
                    .id(jwt.getSubject())
                    .email(jwt.getClaimAsString("email"))
                    .username(jwt.getClaimAsString("preferred_username"))
                    .firstName(jwt.getClaimAsString("given_name"))
                    .lastName(jwt.getClaimAsString("family_name"))
                    .roles(extractRoles(jwt))
                    .permissions(new ArrayList<>())
                    .build();
                
                return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken((String) responseBody.get("refresh_token"))
                    .tokenType("Bearer")
                    .expiresIn(((Number) responseBody.get("expires_in")).longValue())
                    .user(userInfo)
                    .build();
            }
            
            throw new AuthException("Falha ao autenticar com Keycloak");
            
        } catch (AuthException e) {
            // Propagar AuthException sem modificar a mensagem
            throw e;
        } catch (Exception e) {
            log.error("Erro ao fazer login: {}", e.getMessage());
            throw new AuthException("Credenciais inválidas", e);
        }
    }
    
    public TokenResponse refresh(RefreshRequest request) {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", request.getRefreshToken());
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                return TokenResponse.builder()
                    .accessToken((String) responseBody.get("access_token"))
                    .refreshToken((String) responseBody.get("refresh_token"))
                    .tokenType("Bearer")
                    .expiresIn(((Number) responseBody.get("expires_in")).longValue())
                    .build();
            }
            
            throw new AuthException("Falha ao renovar token");
            
        } catch (Exception e) {
            log.error("Erro ao renovar token: {}", e.getMessage());
            throw new AuthException("Refresh token inválido ou expirado", e);
        }
    }
    
    public TokenResponse register(RegisterRequest request, String ipAddress) {
        try {
            // Criar usuário no Keycloak e obter o userId
            String userId = keycloakAdminService.createUser(request);
            
            // Enviar email de verificação
            try {
                keycloakAdminService.sendVerificationEmail(userId);
                log.info("Email de verificação enviado para: {}", request.getEmail());
            } catch (Exception e) {
                log.warn("Erro ao enviar email de verificação: {}", e.getMessage());
                // Não falhar o registro se o email não puder ser enviado
            }
            
            // Fazer login automático após registro
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(request.getEmail());
            loginRequest.setPassword(request.getPassword());
            
            return login(loginRequest, ipAddress);
            
        } catch (Exception e) {
            log.error("Erro ao registrar utilizador: {}", e.getMessage());
            throw new AuthException("Erro ao registrar utilizador: " + e.getMessage(), e);
        }
    }
    
    public TokenResponse processOAuthCallback(String code, String ipAddress) {
        try {
            log.info("Processando callback OAuth2 com código: {}", code.substring(0, Math.min(10, code.length())) + "...");
            
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);
            body.add("redirect_uri", "http://localhost:4200/auth/callback");
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");
                
                // Decodificar o token e sincronizar usuário
                TokenResponse.UserInfo userInfo = null;
                Jwt jwt = jwtDecoder.decode(accessToken);
                
                // Sincronizar e validar usuário (lança exceção se inativo)
                syncUserFromJwt(jwt, ipAddress);
                
                // Extrair informações do usuário do JWT
                userInfo = TokenResponse.UserInfo.builder()
                    .id(jwt.getSubject())
                    .email(jwt.getClaimAsString("email"))
                    .username(jwt.getClaimAsString("preferred_username"))
                    .firstName(jwt.getClaimAsString("given_name"))
                    .lastName(jwt.getClaimAsString("family_name"))
                    .roles(extractRoles(jwt))
                    .permissions(new ArrayList<>())
                    .build();
                
                return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken((String) responseBody.get("refresh_token"))
                    .tokenType("Bearer")
                    .expiresIn(((Number) responseBody.get("expires_in")).longValue())
                    .user(userInfo)
                    .build();
            }
            
            throw new AuthException("Falha ao processar callback OAuth2");
            
        } catch (AuthException e) {
            // Propagar AuthException sem modificar a mensagem
            throw e;
        } catch (Exception e) {
            log.error("Erro ao processar callback OAuth2: {}", e.getMessage(), e);
            throw new AuthException("Erro ao processar autenticação OAuth2", e);
        }
    }
    
    public void logout(String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("Tentativa de logout sem refresh token");
                return;
            }
            
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", 
                keycloakUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                logoutUrl, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT || 
                response.getStatusCode() == HttpStatus.OK) {
                log.info("Logout realizado com sucesso no Keycloak");
            } else {
                log.warn("Resposta inesperada do Keycloak no logout: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Erro ao fazer logout no Keycloak: {}", e.getMessage());
            // Não lançar exceção para não bloquear o logout no frontend
        }
    }
    
    public AuthUserResponse getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
                throw new AuthException("Utilizador não autenticado");
            }
            
            Jwt jwt = (Jwt) authentication.getPrincipal();
            
            return AuthUserResponse.builder()
                .id(jwt.getSubject())
                .name(jwt.getClaimAsString("name"))
                .email(jwt.getClaimAsString("email"))
                .emailVerified(jwt.getClaimAsBoolean("email_verified"))
                .roles(extractRoles(jwt))
                .build();
                
        } catch (Exception e) {
            log.error("Erro ao obter utilizador atual: {}", e.getMessage());
            throw new AuthException("Erro ao obter dados do utilizador", e);
        }
    } 
    
    public void updatePassword(String currentPassword, String newPassword, String confirmPassword) {
        try {
            // Validar se as senhas coincidem
            if (!newPassword.equals(confirmPassword)) {
                throw new AuthException("Nova senha e confirmação não coincidem");
            }
            
            // Obter usuário atual
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
                throw new AuthException("Utilizador não autenticado");
            }
            
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userId = jwt.getSubject();
            
            // Atualizar senha via Keycloak Admin
            keycloakAdminService.updateUserPassword(userId, currentPassword, newPassword);
            
            log.info("Senha atualizada com sucesso para o utilizador: {}", userId);
            
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao atualizar senha: {}", e.getMessage());
            throw new AuthException("Erro ao atualizar senha", e);
        }
    }
    
    public AuthUserResponse updateUserInfo(String name, String email, String phone, String company, String jobTitle) {
        try {
            // Obter usuário atual
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
                throw new AuthException("Utilizador não autenticado");
            }
            
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userId = jwt.getSubject();
            
            // Atualizar informações via Keycloak Admin
            keycloakAdminService.updateUserInfo(userId, name, email, phone, company, jobTitle);
            
            // Retornar informações atualizadas
            Map<String, Object> userInfo = keycloakAdminService.getUserInfo(userId);
            
            return AuthUserResponse.builder()
                .id(userId)
                .name(userInfo.get("firstName") + " " + userInfo.getOrDefault("lastName", ""))
                .email((String) userInfo.get("email"))
                .emailVerified((Boolean) userInfo.getOrDefault("emailVerified", false))
                .roles(extractRoles(jwt))
                .build();
            
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao atualizar informações: {}", e.getMessage());
            throw new AuthException("Erro ao atualizar informações do utilizador", e);
        }
    }
    
    /**
     * Sincroniza usuário do JWT para a base de dados
     */
    private void syncUserFromJwt(Jwt jwt, String ipAddress) {
        try {
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");
            String username = jwt.getClaimAsString("preferred_username");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            
            // Extrair roles do JWT
            List<String> roleCodes = extractRoles(jwt);
            
            // Se não tiver nome, usar o nome do email
            if (firstName == null || firstName.isEmpty()) {
                firstName = email != null ? email.split("@")[0] : "User";
            }
            if (lastName == null || lastName.isEmpty()) {
                lastName = "";
            }
            
            log.info("Sincronizando usuário após autenticação: {} ({})", email, keycloakId);
            log.info("Roles extraídas do JWT: {}", roleCodes);
            
            var user = userService.createOrUpdateUser(
                keycloakId, 
                email, 
                firstName, 
                lastName, 
                username, 
                emailVerified,
                roleCodes
            );
            
            // Verificar se o usuário está ativo
            if (!user.getActive()) {
                log.warn("Tentativa de login de usuário inativo: {} ({})", email, keycloakId);
                throw new AuthException("Sua conta está inativa. Entre em contato com o administrador.");
            }
            
            // Atualizar último login
            if (ipAddress != null && user.getId() != null) {
                userService.updateLastLogin(user.getId(), ipAddress);
            }
            
        } catch (AuthException e) {
            // Propagar exceção de autenticação
            throw e;
        } catch (Exception e) {
            log.error("Erro ao sincronizar usuário do JWT: {}", e.getMessage(), e);
            throw new AuthException("Erro ao processar autenticação", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            // Tentar extrair de realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                log.debug("Roles encontradas em realm_access: {}", roles);
                return roles;
            }
            
            // Tentar extrair de resource_access
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> resource = (Map<String, Object>) entry.getValue();
                        if (resource.containsKey("roles")) {
                            List<String> roles = (List<String>) resource.get("roles");
                            log.debug("Roles encontradas em resource_access.{}: {}", entry.getKey(), roles);
                            return roles;
                        }
                    }
                }
            }
            
            log.warn("Nenhuma role encontrada no JWT");
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Erro ao extrair roles do JWT: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Reenvia email de verificação para o usuário autenticado
     */
    public void resendVerificationEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
                throw new AuthException("Utilizador não autenticado");
            }
            
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userId = jwt.getSubject();
            
            keycloakAdminService.sendVerificationEmail(userId);
            log.info("Email de verificação reenviado para o usuário: {}", userId);
            
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao reenviar email de verificação: {}", e.getMessage());
            throw new AuthException("Erro ao reenviar email de verificação", e);
        }
    }
}
    
