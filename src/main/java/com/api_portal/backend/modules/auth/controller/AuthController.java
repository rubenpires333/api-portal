package com.api_portal.backend.modules.auth.controller;

import com.api_portal.backend.modules.auth.dto.*;
import com.api_portal.backend.modules.auth.service.AuthService;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação e autorização")
public class AuthController {
    
    private final AuthService authService;
    
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
}
