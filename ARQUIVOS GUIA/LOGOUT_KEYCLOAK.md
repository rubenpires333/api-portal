# Logout Real no Keycloak

## Implementação

### Backend - AuthController.java

```java
@PostMapping("/logout")
public ResponseEntity<String> logout(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String refreshToken = authHeader.substring(7);
        authService.logout(refreshToken);
    }
    
    return ResponseEntity.ok("Logout realizado com sucesso");
}
```

### Backend - AuthService.java

```java
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
        }
        
    } catch (Exception e) {
        log.error("Erro ao fazer logout no Keycloak: {}", e.getMessage());
        // Não lançar exceção para não bloquear o logout no frontend
    }
}
```

### Frontend - auth.service.ts

```typescript
logout(): void {
    const refreshToken = this.getRefreshToken()
    
    // Chamar endpoint de logout no backend com refresh token
    if (refreshToken) {
      this.http.post(`${environment.apiUrl}/auth/logout`, {}, {
        headers: {
          'Authorization': `Bearer ${refreshToken}`
        }
      }).subscribe({
        next: () => {
          console.log('Logout realizado no Keycloak')
        },
        error: (err) => {
          console.warn('Erro ao fazer logout no Keycloak:', err)
        }
      })
    }
    
    // Remover sessão local
    this.removeSession()
    this.currentUserSubject.next(null)
}
```

## Como Funciona

### 1. Usuário Clica em Logout

```
Frontend: authService.logout()
```

### 2. Frontend Envia Refresh Token

```
POST /api/v1/auth/logout
Headers:
  Authorization: Bearer <refresh_token>
```

### 3. Backend Invalida Token no Keycloak

```
POST http://localhost:8180/realms/portal-api/protocol/openid-connect/logout
Body:
  client_id=portal-api
  client_secret=<secret>
  refresh_token=<refresh_token>
```

### 4. Keycloak Invalida Sessão

- Remove refresh token da lista de tokens válidos
- Invalida sessão do usuário
- Access token continua válido até expirar (mas refresh não funciona mais)

### 5. Frontend Limpa Sessão Local

```typescript
this.removeSession()  // Remove tokens do localStorage/cookies
this.currentUserSubject.next(null)  // Limpa usuário atual
```

### 6. Redirecionamento

```typescript
this.router.navigate(['/auth/sign-in'])
```

## Vantagens do Logout Real

### ✅ Segurança
- Token é invalidado no servidor (Keycloak)
- Mesmo que alguém tenha o refresh token, não consegue renovar
- Sessão é completamente encerrada

### ✅ Single Sign-Out (SSO)
- Se o usuário fizer logout em uma aplicação, logout em todas
- Keycloak gerencia sessões centralizadas

### ✅ Auditoria
- Keycloak registra quando usuário fez logout
- Logs de segurança completos

## Diferença: Logout Frontend vs Backend

### Logout Apenas Frontend (Antigo)
```
❌ Token continua válido no Keycloak
❌ Refresh token ainda funciona
❌ Sessão ativa no servidor
✅ Rápido (sem chamada HTTP)
```

### Logout Real no Keycloak (Novo)
```
✅ Token invalidado no servidor
✅ Refresh token não funciona mais
✅ Sessão encerrada no Keycloak
✅ Mais seguro
⚠️ Requer chamada HTTP (pequeno delay)
```

## Testando

### 1. Fazer Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Resposta:**
```json
{
  "accessToken": "...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600
}
```

### 2. Fazer Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Resposta:**
```
Logout realizado com sucesso
```

### 3. Tentar Renovar Token (Deve Falhar)

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

**Resposta esperada:**
```json
{
  "error": "invalid_grant",
  "error_description": "Token is not active"
}
```

## Configuração Keycloak

### Client Settings

```
Client ID: portal-api
Client Protocol: openid-connect
Access Type: confidential
Standard Flow Enabled: ON
Direct Access Grants Enabled: ON
Service Accounts Enabled: ON

Valid Redirect URIs:
  - http://localhost:4200/*
  
Web Origins:
  - http://localhost:4200
```

### Client Secret

1. Ir em Clients > portal-api > Credentials
2. Copiar Client Secret
3. Adicionar no `application.properties`:

```properties
keycloak.client-secret=<seu-client-secret>
```

## Troubleshooting

### Erro: "Token is not active"

**Causa**: Refresh token já foi usado ou expirou.

**Solução**: Fazer login novamente.

### Erro: "Invalid client credentials"

**Causa**: Client secret incorreto no `application.properties`.

**Solução**: Verificar client secret no Keycloak.

### Logout não invalida token

**Causa**: Refresh token não está sendo enviado.

**Solução**: Verificar se frontend está enviando refresh token no header Authorization.

### Access token ainda funciona após logout

**Comportamento esperado**: Access token continua válido até expirar (geralmente 5-15 minutos). Mas o refresh token não funciona mais, então quando o access token expirar, o usuário precisa fazer login novamente.

**Solução alternativa**: Implementar blacklist de tokens no backend (mais complexo).

## Melhorias Futuras

### 1. Blacklist de Access Tokens

Armazenar access tokens invalidados em Redis:

```java
@Service
public class TokenBlacklistService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public void blacklistToken(String token, long expiresIn) {
        redisTemplate.opsForValue().set(
            "blacklist:" + token, 
            "true", 
            expiresIn, 
            TimeUnit.SECONDS
        );
    }
    
    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}
```

### 2. Logout de Todas as Sessões

Endpoint para invalidar todas as sessões do usuário:

```java
@PostMapping("/logout-all")
public ResponseEntity<String> logoutAll() {
    String userId = getCurrentUserId();
    keycloakAdminService.logoutAllSessions(userId);
    return ResponseEntity.ok("Todas as sessões foram encerradas");
}
```

### 3. Notificação de Logout

WebSocket para notificar outras abas/dispositivos:

```typescript
// Quando logout acontece
this.websocket.send({
  type: 'LOGOUT',
  userId: currentUser.id
})

// Outras abas recebem e fazem logout local
this.websocket.on('LOGOUT', (data) => {
  if (data.userId === currentUser.id) {
    this.localLogout()
  }
})
```

---

**Status**: ✅ Logout real no Keycloak implementado  
**Data**: 28 de Março de 2026, 17:00
