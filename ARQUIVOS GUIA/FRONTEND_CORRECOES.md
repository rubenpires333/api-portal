# Correções Frontend - Integração com Backend Existente

## Problemas Corrigidos

### 1. Erro: Property 'session' does not exist
**Causa**: O código antigo usava `authService.session` mas o novo service não tinha essa propriedade.

**Solução**: Adicionado getter `session` no `AuthenticationService`:
```typescript
get session(): string {
  return this.getToken() || ''
}
```

### 2. Erro: Type 'LoginResponse' has no properties in common with type 'User'
**Causa**: O NgRx store esperava tipo `User` mas o service retornava `LoginResponse`.

**Solução**: Modificado o método `login()` para retornar `Observable<User>` ao invés de `Observable<LoginResponse>`, fazendo a conversão internamente:
```typescript
login(email: string, password: string): Observable<User> {
  return this.http.post<LoginResponse>(...).pipe(
    map((response) => {
      const user: User = {
        id: response.user?.id || '',
        email: response.user?.email || email,
        // ... conversão completa
      }
      return user
    })
  )
}
```

## Ajustes para Backend Existente

### 1. Configuração do Keycloak
Atualizado `environment.ts` para usar a configuração correta:
```typescript
keycloak: {
  url: 'http://localhost:8180',
  realm: 'portal-api',      // Era 'apicv'
  clientId: 'portal-api'     // Era 'api-portal-frontend'
}
```

### 2. URLs de Autenticação Social
Ajustado para usar Keycloak como proxy para Google e GitHub:

**GitHub via Keycloak:**
```
http://localhost:8180/realms/portal-api/protocol/openid-connect/auth
  ?client_id=portal-api
  &redirect_uri=http://localhost:4200/auth/callback
  &response_type=code
  &scope=openid
  &kc_idp_hint=github
```

**Google via Keycloak:**
```
http://localhost:8180/realms/portal-api/protocol/openid-connect/auth
  ?client_id=portal-api
  &redirect_uri=http://localhost:4200/auth/callback
  &response_type=code
  &scope=openid
  &kc_idp_hint=google
```

### 3. Endpoint de Callback Unificado
Removida rota `/auth/callback/google` - agora usa apenas `/auth/callback` para ambos providers.

O backend diferencia pelo parâmetro `kc_idp_hint` na URL de autenticação.

### 4. Mapeamento de Roles
Criado método para mapear roles do backend para o tipo `User`:
```typescript
private mapRoleToUserRole(roles: string[]): 'admin' | 'user' {
  if (roles.includes('SUPER_ADMIN') || roles.includes('ADMIN')) {
    return 'admin'
  }
  return 'user'
}
```

## Fluxo de Autenticação Atualizado

### Login Tradicional
```
1. Frontend: POST /api/v1/auth/login { email, password }
2. Backend: Valida credenciais
3. Backend: Retorna { access_token, refresh_token, user }
4. Frontend: Converte para tipo User e salva sessão
5. Frontend: Redireciona baseado na role
```

### Login Social (GitHub/Google)
```
1. Frontend: Redireciona para Keycloak com kc_idp_hint
2. Keycloak: Redireciona para provider (GitHub/Google)
3. Provider: Usuário autoriza
4. Provider: Redireciona para Keycloak
5. Keycloak: Redireciona para /auth/callback?code=XXX
6. Frontend: Extrai código
7. Frontend: POST /api/v1/auth/oauth2/callback { code }
8. Backend: Troca código por token com Keycloak
9. Backend: Retorna { access_token, refresh_token, user }
10. Frontend: Salva sessão e redireciona
```

## Estrutura de Resposta do Backend

O backend deve retornar no seguinte formato:

```typescript
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 3600,
  "token_type": "Bearer",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "username": "user",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["ADMIN", "USER"],
    "permissions": ["api.create", "api.read"]
  }
}
```

## Endpoints Necessários no Backend

### ✅ Já Implementados (conforme informado)
- `POST /api/v1/auth/login` - Login tradicional
- `POST /api/v1/auth/register` - Registro
- `POST /api/v1/auth/oauth2/callback` - Callback OAuth2 (GitHub e Google)

### ⏳ Recomendados para Implementar
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Logout (invalidar token)
- `GET /api/v1/auth/me` - Obter dados do usuário atual

## Compatibilidade com Código Existente

O `AuthenticationService` mantém compatibilidade com o código antigo:

**Métodos mantidos:**
- `session` (getter) - Retorna token
- `saveSession(token, refreshToken, user)` - Salva sessão
- `removeSession()` - Remove sessão
- `logout()` - Faz logout

**Novos métodos:**
- `loginWithGithub()` - Login via GitHub
- `loginWithGoogle()` - Login via Google
- `handleOAuthCallback(code)` - Processa callback OAuth2
- `isAuthenticated()` - Verifica autenticação
- `hasRole(role)` - Verifica role

## Testando

### 1. Compilar Frontend
```bash
cd frontend
npm install
ng serve
```

### 2. Acessar
```
http://localhost:4200/auth/sign-in
```

### 3. Testar Login Tradicional
- Email: seu-email@example.com
- Senha: sua-senha

### 4. Testar Login Social
- Clicar em "GOOGLE" ou "GITHUB"
- Será redirecionado para Keycloak
- Keycloak redireciona para provider
- Após autorizar, volta para /auth/callback
- Frontend processa e redireciona

## Verificar Erros de Compilação

Execute:
```bash
ng build
```

Não deve haver mais erros de:
- `Property 'session' does not exist`
- `Type 'LoginResponse' has no properties in common with type 'User'`

## Próximos Passos

1. ✅ Erros de compilação corrigidos
2. ✅ Integração com backend existente ajustada
3. ✅ Fluxo OAuth2 via Keycloak configurado
4. ⏳ Testar login tradicional
5. ⏳ Testar login com GitHub
6. ⏳ Testar login com Google
7. ⏳ Criar módulos admin/provider/consumer
8. ⏳ Implementar guards nas rotas

---

**Status**: ✅ Correções aplicadas  
**Data**: 28 de Março de 2026
