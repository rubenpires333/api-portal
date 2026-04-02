package com.api_portal.backend.modules.auth.controller;

import com.api_portal.backend.modules.auth.dto.*;
import com.api_portal.backend.modules.auth.service.AuthService;
import com.api_portal.backend.modules.auth.service.LoginAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação e autorização")
public class AuthController {
    
    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;
    
    @PostMapping("/login")
    @Operation(
        summary = "Login de utilizador",
        description = "Autentica um utilizador com email e password, retornando tokens JWT"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        TokenResponse response = authService.login(request, ipAddress);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar token",
        description = "Renova o access token usando o refresh token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    @Operation(
        summary = "Registar novo utilizador",
        description = "Cria um novo utilizador na plataforma (PROVIDER ou CONSUMER)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Utilizador criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já existe"),
        @ApiResponse(responseCode = "500", description = "Erro ao criar utilizador")
    })
    public ResponseEntity<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        TokenResponse response = authService.register(request, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/me")
    @Operation(
        summary = "Obter dados do utilizador autenticado",
        description = "Retorna informações do utilizador atual baseado no token JWT",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dados obtidos com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado")
    })
    public ResponseEntity<AuthUserResponse> getCurrentUser() {
        AuthUserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/password")
    @Operation(
        summary = "Atualizar senha do utilizador",
        description = "Permite ao utilizador autenticado alterar sua senha",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Senha atualizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou senhas não coincidem"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou senha atual incorreta")
    })
    public ResponseEntity<String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(
            request.getCurrentPassword(), 
            request.getNewPassword(), 
            request.getConfirmPassword()
        );
        return ResponseEntity.ok("Senha atualizada com sucesso");
    }
    
    @PutMapping("/profile")
    @Operation(
        summary = "Atualizar informações do utilizador",
        description = "Permite ao utilizador autenticado atualizar suas informações pessoais",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Informações atualizadas com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado")
    })
    public ResponseEntity<AuthUserResponse> updateProfile(@Valid @RequestBody UpdateUserRequest request) {
        AuthUserResponse response = authService.updateUserInfo(
            request.getName(),
            request.getEmail(),
            request.getPhone(), 
            request.getCompany(),
            request.getJobTitle()
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o módulo de autenticação está ativo")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth module is running");
    }
    
    @GetMapping("/captcha-required")
    @Operation(
        summary = "Verificar se CAPTCHA é necessário",
        description = "Verifica se o usuário precisa resolver um CAPTCHA para fazer login"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verificação realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    public ResponseEntity<CaptchaRequirementResponse> checkCaptchaRequirement(
            @RequestParam String email,
            HttpServletRequest httpRequest) {
        
        String ipAddress = httpRequest.getRemoteAddr();
        boolean requiresCaptcha = loginAttemptService.requiresCaptcha(email, ipAddress);
        boolean isBlocked = loginAttemptService.isBlocked(email, ipAddress);
        long blockedMinutes = loginAttemptService.getBlockedMinutesRemaining(email, ipAddress);
        
        String message;
        if (isBlocked) {
            message = String.format("Conta bloqueada. Tente novamente em %d minutos.", blockedMinutes);
        } else if (requiresCaptcha) {
            message = "CAPTCHA necessário após múltiplas tentativas falhadas";
        } else {
            message = "Login normal permitido";
        }
        
        CaptchaRequirementResponse response = CaptchaRequirementResponse.builder()
            .requiresCaptcha(requiresCaptcha)
            .isBlocked(isBlocked)
            .blockedMinutesRemaining(blockedMinutes)
            .message(message)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    @Operation(
        summary = "Logout do utilizador",
        description = "Invalida o token JWT no Keycloak e remove a sessão",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token inválido")
    })
    public ResponseEntity<String> logout(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Tentar obter refresh token do body primeiro, depois do header
        String refreshToken = null;
        
        if (body != null && body.containsKey("refreshToken")) {
            refreshToken = body.get("refreshToken");
        } else if (body != null && body.containsKey("refresh_token")) {
            refreshToken = body.get("refresh_token");
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7);
        }
        
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        
        return ResponseEntity.ok("Logout realizado com sucesso");
    }
    
    @PostMapping("/oauth2/callback")
    @Operation(
        summary = "Callback OAuth2",
        description = "Processa o callback do Keycloak após autenticação social (Google, GitHub)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticação OAuth2 realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Código inválido ou ausente"),
        @ApiResponse(responseCode = "500", description = "Erro ao processar OAuth2")
    })
    public ResponseEntity<TokenResponse> oauthCallback(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletRequest httpRequest) {
        
        // Se houver erro do provider OAuth
        if (error != null) {
            throw new RuntimeException("OAuth error: " + error + " - " + error_description);
        }
        
        // Tentar obter código do body ou query parameter
        String authCode = code;
        if (authCode == null && body != null) {
            authCode = body.get("code");
        }
        
        // Se não houver código, retornar erro
        if (authCode == null || authCode.isEmpty()) {
            throw new RuntimeException("Authorization code is required");
        }
        
        String ipAddress = httpRequest.getRemoteAddr();
        TokenResponse response = authService.processOAuthCallback(authCode, ipAddress);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/verify-email")
    @Operation(
        summary = "Verificar email",
        description = "Verifica o email do utilizador através do token enviado por email"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verificado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    })
    public ResponseEntity<String> verifyEmail(
            @RequestParam String token,
            @RequestParam(required = false) String userId) {
        
        // O Keycloak processa o token automaticamente
        // Este endpoint é apenas para redirecionar o usuário após a verificação
        return ResponseEntity.ok("Email verificado com sucesso! Você pode fechar esta janela.");
    }
    
    @PostMapping("/resend-verification")
    @Operation(
        summary = "Reenviar email de verificação",
        description = "Reenvia o email de verificação para o utilizador autenticado",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email de verificação reenviado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<String> resendVerificationEmail() {
        authService.resendVerificationEmail();
        return ResponseEntity.ok("Email de verificação enviado");
    }
}
