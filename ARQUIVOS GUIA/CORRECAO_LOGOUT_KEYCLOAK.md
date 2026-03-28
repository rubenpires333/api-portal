# Correção: Logout no Keycloak - Invalid Refresh Token

## Data
28 de Março de 2026

## Problema

Ao fazer logout, o backend retornava erro:

```
ERROR: Erro ao fazer logout no Keycloak: 400 Bad Request on POST request for 
"http://localhost:8180/realms/portal-api/protocol/openid-connect/logout": 
"{"error":"invalid_grant","error_description":"Invalid refresh token"}"
```

## Causa Raiz

O frontend estava enviando o **refresh token no header Authorization**, mas o Keycloak espera receber o refresh token no **body da requisição** como `refresh_token`.

### Fluxo Incorreto (Antes)

```
Frontend:
POST /api/v1/auth/logout
Headers:
  Authorization: Bearer <refresh_token>
Body: {}

Backend:
POST http://localhost:8180/realms/portal-api/protocol/openid-connect/logout
Body:
  client_id=portal-api
  client_secret=<secret>
  refresh_token=<refresh_token>  ❌ Token estava vindo do header, não do body
```

## Solução

### 1. Atualizado AuthController.java

Agora aceita refresh token tanto no body quanto no header (para compatibilidade):

```java
@PostMapping("/logout")
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
```

### 2. Atualizado auth.service.ts (Frontend)

Agora envia refresh token no body:

```typescript
logout(): void {
    const refreshToken = this.getRefreshToken()
    
    // Chamar endpoint de logout no backend com refresh token no body
    if (refreshToken) {
      this.http.post(`${environment.apiUrl}/auth/logout`, {
        refreshToken: refreshToken  // ✅ Enviando no body
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

## Fluxo Correto (Depois)

```
Frontend:
POST /api/v1/auth/logout
Body:
  {
    "refreshToken": "<refresh_token>"
  }

Backend:
POST http://localhost:8180/realms/portal-api/protocol/openid-connect/logout
Body:
  client_id=portal-api
  client_secret=<secret>
  refresh_token=<refresh_token>  ✅ Token correto do body

Keycloak:
200 OK ou 204 No Content
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
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 300,
  "user": {
    "id": "1bbb401e-8dd2-4de7-ad19-d2b0c1d82769",
    "email": "user@example.com",
    "roles": ["CONSUMER"]
  }
}
```

### 2. Fazer Logout (Método Correto)

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

**Resposta esperada:**
```
Logout realizado com sucesso
```

**Logs do backend:**
```
INFO: Logout realizado com sucesso no Keycloak
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

## Compatibilidade

O backend agora aceita refresh token de 3 formas:

### 1. Body com camelCase (Recomendado)
```json
{
  "refreshToken": "..."
}
```

### 2. Body com snake_case
```json
{
  "refresh_token": "..."
}
```

### 3. Header Authorization (Fallback)
```
Authorization: Bearer <refresh_token>
```

## Diferenças: Access Token vs Refresh Token

### Access Token
- Usado para autenticar requisições à API
- Curta duração (5-15 minutos)
- Enviado no header: `Authorization: Bearer <access_token>`
- Não pode ser revogado diretamente (expira naturalmente)

### Refresh Token
- Usado para renovar access token
- Longa duração (horas/dias)
- Enviado no body de requisições específicas (refresh, logout)
- Pode ser revogado no Keycloak

## Por Que Isso Aconteceu?

### Confusão Comum

Muitos desenvolvedores confundem:
- **Access token** → Sempre no header `Authorization: Bearer`
- **Refresh token** → No body de requisições específicas

### Documentação Keycloak

A documentação do Keycloak especifica claramente:

```
POST /realms/{realm}/protocol/openid-connect/logout
Content-Type: application/x-www-form-urlencoded

Body:
  client_id=<client_id>
  client_secret=<client_secret>
  refresh_token=<refresh_token>  ← No body, não no header!
```

## Lições Aprendidas

1. **Refresh token vai no body**, não no header
2. Access token vai no header `Authorization: Bearer`
3. Sempre verificar documentação oficial do Keycloak
4. Logs detalhados ajudam a identificar problemas rapidamente
5. Suportar múltiplos formatos (camelCase, snake_case) melhora compatibilidade

## Próximos Passos

- [x] Corrigido envio de refresh token no frontend
- [x] Atualizado backend para aceitar múltiplos formatos
- [x] Testado logout com sucesso
- [ ] Testar logout após login com Google
- [ ] Testar logout após login com GitHub
- [ ] Implementar blacklist de access tokens (opcional)

---

**Status**: ✅ Logout funcionando corretamente  
**Erro**: Invalid refresh token - RESOLVIDO  
**Método**: Refresh token agora é enviado no body da requisição
