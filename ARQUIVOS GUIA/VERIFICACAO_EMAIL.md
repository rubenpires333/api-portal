# Verificação de Email - Implementação Completa

## Data
28 de Março de 2026

## Visão Geral

Sistema completo de verificação de email integrado com Keycloak, incluindo:
- Envio automático de email após registro
- Página de confirmação de verificação
- Reenvio de email de verificação
- Verificação manual via backend

## Arquitetura

```
Registro → Keycloak cria usuário → Backend envia email → 
Usuário clica no link → Keycloak verifica → Redireciona para frontend →
Frontend exibe sucesso → Redireciona para login
```

## Backend

### 1. KeycloakAdminService.java

**Método: sendVerificationEmail**
```java
public void sendVerificationEmail(String userId) {
    String verifyEmailUrl = String.format(
        "%s/admin/realms/%s/users/%s/send-verify-email", 
        keycloakUrl, realm, userId
    );
    
    String redirectUri = "http://localhost:4200/auth/email-verified";
    verifyEmailUrl += "?redirect_uri=" + redirectUri;
    
    // Envia requisição PUT para Keycloak
    // Keycloak envia email com link de verificação
}
```

**Método: verifyEmail (Manual)**
```java
public void verifyEmail(String userId) {
    // Atualiza emailVerified = true no Keycloak
    // Usado quando o link do email não funciona
}
```

### 2. AuthService.java

**Método: register (Atualizado)**
```java
public TokenResponse register(RegisterRequest request, String ipAddress) {
    // 1. Criar usuário no Keycloak
    String userId = keycloakAdminService.createUser(request);
    
    // 2. Enviar email de verificação
    keycloakAdminService.sendVerificationEmail(userId);
    
    // 3. Fazer login automático
    return login(loginRequest, ipAddress);
}
```

**Método: resendVerificationEmail**
```java
public void resendVerificationEmail() {
    // Obtém userId do JWT do usuário autenticado
    // Reenvia email de verificação
}
```

### 3. AuthController.java

**Endpoint: GET /api/v1/auth/verify-email**
```java
@GetMapping("/verify-email")
public ResponseEntity<String> verifyEmail(
    @RequestParam String token,
    @RequestParam(required = false) String userId
) {
    // Keycloak processa o token automaticamente
    // Este endpoint apenas confirma o sucesso
    return ResponseEntity.ok("Email verificado com sucesso!");
}
```

**Endpoint: POST /api/v1/auth/resend-verification**
```java
@PostMapping("/resend-verification")
@SecurityRequirement(name = "Bearer Authentication")
public ResponseEntity<String> resendVerificationEmail() {
    authService.resendVerificationEmail();
    return ResponseEntity.ok("Email de verificação enviado");
}
```

## Frontend

### 1. EmailVerifiedComponent

**Localização:** `frontend/src/app/views/auth/email-verified/`

**Funcionalidades:**
- Exibe mensagem de sucesso após verificação
- Exibe mensagem de erro se verificação falhar
- Redireciona automaticamente para login após 5 segundos
- Botão manual para ir para login

**Template:**
```html
<!-- Sucesso -->
<div *ngIf="success">
  <i class="ri-checkbox-circle-line text-success"></i>
  <h2>Email Verificado!</h2>
  <p>Você será redirecionado em 5 segundos...</p>
  <button (click)="goToLogin()">Ir para Login</button>
</div>

<!-- Erro -->
<div *ngIf="error">
  <i class="ri-error-warning-line text-danger"></i>
  <h2>Erro na Verificação</h2>
  <p>{{ error }}</p>
  <button (click)="goToLogin()">Voltar para Login</button>
</div>
```

### 2. Rota

**Arquivo:** `frontend/src/app/views/auth/auth.route.ts`

```typescript
{
  path: 'email-verified',
  component: EmailVerifiedComponent,
  data: { title: 'Email Verified' },
}
```

## Fluxo Completo

### 1. Usuário se Registra

```
Frontend: POST /api/v1/auth/register
Body: {
  "firstName": "João",
  "lastName": "Silva",
  "email": "joao@example.com",
  "password": "password123"
}
```

### 2. Backend Cria Usuário

```
1. KeycloakAdminService.createUser()
   - Cria usuário no Keycloak
   - emailVerified: false
   - Retorna userId

2. KeycloakAdminService.sendVerificationEmail(userId)
   - Envia requisição para Keycloak
   - Keycloak envia email para joao@example.com
```

### 3. Email Enviado

**Assunto:** Verify email

**Conteúdo:**
```
Olá João,

Por favor, verifique seu email clicando no link abaixo:

[Verificar Email]

Este link expira em 24 horas.
```

**Link:**
```
http://localhost:8180/realms/portal-api/login-actions/action-token?
  key=<token>&
  client_id=portal-api&
  tab_id=<tab_id>&
  execution=VERIFY_EMAIL
```

### 4. Usuário Clica no Link

```
1. Keycloak processa o token
2. Atualiza emailVerified = true
3. Redireciona para: http://localhost:4200/auth/email-verified?success=true
```

### 5. Frontend Exibe Sucesso

```
EmailVerifiedComponent:
- Detecta success=true na URL
- Exibe mensagem de sucesso
- Aguarda 5 segundos
- Redireciona para /auth/sign-in
```

## Configuração do Keycloak

### 1. Habilitar Verificação de Email

```
Realm Settings > Login:
  ✓ Verify email
  ✓ Email as username (opcional)
```

### 2. Configurar SMTP

```
Realm Settings > Email:
  Host: smtp.gmail.com
  Port: 587
  From: noreply@example.com
  Enable StartTLS: ON
  Enable Authentication: ON
  Username: seu-email@gmail.com
  Password: sua-senha-app
```

**Gmail App Password:**
1. Acessar: https://myaccount.google.com/apppasswords
2. Criar senha de app
3. Usar no Keycloak

### 3. Personalizar Email Template

**Localização:** `themes/base/email/html/email-verification.ftl`

```html
<#import "template.ftl" as layout>
<@layout.emailLayout>
  <h1>Verificar Email</h1>
  <p>Olá ${user.firstName},</p>
  <p>Por favor, verifique seu email clicando no link abaixo:</p>
  <p><a href="${link}">Verificar Email</a></p>
  <p>Este link expira em ${linkExpirationFormatter(linkExpiration)}.</p>
</@layout.emailLayout>
```

## Reenvio de Email

### Frontend

```typescript
// AuthService
resendVerificationEmail(): Observable<string> {
  return this.http.post<string>(
    `${environment.apiUrl}/auth/resend-verification`,
    {}
  )
}

// Componente
resendEmail() {
  this.authService.resendVerificationEmail().subscribe({
    next: () => {
      this.message = 'Email de verificação enviado!'
    },
    error: (err) => {
      this.error = 'Erro ao enviar email'
    }
  })
}
```

### Backend

```java
@PostMapping("/resend-verification")
public ResponseEntity<String> resendVerificationEmail() {
    // Obtém userId do token JWT
    // Reenvia email
    authService.resendVerificationEmail();
    return ResponseEntity.ok("Email enviado");
}
```

## Verificação Manual (Admin)

Para verificar email manualmente sem enviar email:

```java
// KeycloakAdminService
public void verifyEmail(String userId) {
    // PUT /admin/realms/{realm}/users/{userId}
    // Body: { "emailVerified": true }
}
```

**Uso:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/users/{userId}/verify-email \
  -H "Authorization: Bearer <admin-token>"
```

## Testando

### 1. Testar Registro com Email

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Verificar logs:**
```
INFO: Email de verificação enviado para: <userId>
```

### 2. Verificar Email no Keycloak

```
1. Acessar: http://localhost:8180/admin
2. Realm: portal-api
3. Users > test@example.com
4. Verificar: Email Verified = false
```

### 3. Simular Clique no Link

```
http://localhost:4200/auth/email-verified?success=true
```

### 4. Testar Reenvio

```bash
curl -X POST http://localhost:8080/api/v1/auth/resend-verification \
  -H "Authorization: Bearer <access-token>"
```

## Troubleshooting

### Email Não Chega

**Causas:**
1. SMTP não configurado
2. Credenciais inválidas
3. Email na pasta de spam
4. Firewall bloqueando porta 587

**Solução:**
```
1. Verificar configuração SMTP no Keycloak
2. Testar envio de email de teste
3. Verificar logs do Keycloak
4. Usar MailHog para desenvolvimento
```

### Link Expirado

**Causa:** Token de verificação expirou (padrão: 24 horas)

**Solução:**
```
1. Reenviar email de verificação
2. Ou verificar manualmente via admin
```

### Redirect URI Inválido

**Causa:** URL de redirecionamento não está configurada no Keycloak

**Solução:**
```
Client Settings > Valid Redirect URIs:
  - http://localhost:4200/*
  - http://localhost:4200/auth/email-verified
```

### Email Verificado Mas Ainda Aparece Como Não Verificado

**Causa:** Cache do JWT

**Solução:**
```
1. Fazer logout
2. Fazer login novamente
3. JWT atualizado terá emailVerified: true
```

## Melhorias Futuras

### 1. Notificação no Frontend

```typescript
// Após login, verificar se email está verificado
if (!user.emailVerified) {
  this.showBanner('Por favor, verifique seu email')
}
```

### 2. Bloquear Acesso Sem Verificação

```java
// SecurityConfig
.authorizeHttpRequests(auth -> auth
  .requestMatchers("/api/v1/public/**").permitAll()
  .anyRequest().access((auth, context) -> {
    Jwt jwt = (Jwt) auth.getPrincipal();
    Boolean emailVerified = jwt.getClaim("email_verified");
    return emailVerified != null && emailVerified;
  })
)
```

### 3. Email de Boas-Vindas

```java
// Após verificação, enviar email de boas-vindas
if (emailVerified) {
  emailService.sendWelcomeEmail(user);
}
```

### 4. Lembrete de Verificação

```java
// Cron job para enviar lembrete após 3 dias
@Scheduled(cron = "0 0 9 * * *")
public void sendVerificationReminders() {
  List<User> unverified = userRepository.findByEmailVerifiedFalse();
  // Enviar lembretes
}
```

## Arquivos Modificados

### Backend
- `KeycloakAdminService.java` - Métodos de envio e verificação
- `AuthService.java` - Integração com registro
- `AuthController.java` - Endpoints de verificação
- `SecurityConfig.java` - Permitir acesso ao endpoint

### Frontend
- `email-verified.component.ts` - Componente de sucesso
- `email-verified.component.html` - Template
- `auth.route.ts` - Rota adicionada

---

**Status**: ✅ Verificação de email implementada  
**Próximo**: Configurar SMTP no Keycloak e testar envio de email
