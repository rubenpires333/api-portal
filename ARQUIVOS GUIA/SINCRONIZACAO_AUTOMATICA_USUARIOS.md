# Sincronização Automática de Usuários

## Visão Geral

O sistema agora sincroniza automaticamente os usuários do Keycloak para a base de dados PostgreSQL após login ou autenticação OAuth2. Não é mais necessário chamar manualmente o endpoint `/api/v1/users/me` para criar o usuário na base de dados.

## Como Funciona

### 1. Login Normal

Quando um usuário faz login através do endpoint `/api/v1/auth/login`:

```
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "senha123"
}
```

**Fluxo interno:**
1. Backend autentica com Keycloak
2. Keycloak retorna token JWT
3. Backend decodifica o token JWT
4. Backend extrai informações do usuário:
   - `keycloakId` (subject do JWT)
   - `email`
   - `firstName` (given_name)
   - `lastName` (family_name)
   - `username` (preferred_username)
   - `emailVerified`
5. Backend cria/atualiza usuário na tabela `users`
6. Backend registra IP do cliente e data/hora do último login
7. Backend retorna token para o frontend

### 2. Registro de Novo Usuário

Quando um novo usuário se registra através do endpoint `/api/v1/auth/register`:

```
POST /api/v1/auth/register
{
  "email": "newuser@example.com",
  "password": "senha123",
  "firstName": "João",
  "lastName": "Silva",
  "role": "CONSUMER"
}
```

**Fluxo interno:**
1. Backend cria usuário no Keycloak
2. Backend adiciona usuário ao grupo correspondente (CONSUMER, PROVIDER, SUPER_ADMIN)
3. Backend faz login automático
4. Backend sincroniza usuário (mesmo fluxo do login)
5. Backend retorna token para o frontend

### 3. Autenticação OAuth2

Quando um usuário autentica via OAuth2 (Google, LinkedIn, GitHub):

**Passo 1: Obter URL de autenticação**
```
GET /api/v1/auth/oauth2/login/google
```

Resposta:
```json
{
  "provider": "google",
  "authUrl": "https://keycloak:8180/realms/apicv/protocol/openid-connect/auth?..."
}
```

**Passo 2: Frontend redireciona usuário para authUrl**

**Passo 3: Usuário autentica no provedor (Google, LinkedIn, etc)**

**Passo 4: Provedor redireciona para frontend com código**
```
http://localhost:4200/auth/callback?code=abc123...
```

**Passo 5: Frontend envia código para backend**
```
POST /api/v1/auth/oauth2/callback
{
  "code": "abc123...",
  "redirectUri": "http://localhost:4200/auth/callback"
}
```

**Fluxo interno:**
1. Backend troca código por token JWT no Keycloak
2. Backend decodifica o token JWT
3. Backend extrai informações do usuário
4. Backend cria/atualiza usuário na tabela `users`
5. Backend registra IP do cliente e data/hora do último login
6. Backend retorna token para o frontend

## Informações Sincronizadas

Os seguintes dados são extraídos do JWT e salvos na base de dados:

| Campo JWT | Campo na Base de Dados | Descrição |
|-----------|------------------------|-----------|
| `sub` | `keycloak_id` | ID único do usuário no Keycloak |
| `email` | `email` | Email do usuário |
| `given_name` | `first_name` | Primeiro nome |
| `family_name` | `last_name` | Sobrenome |
| `preferred_username` | `username` | Nome de usuário |
| `email_verified` | `email_verified` | Se o email foi verificado |
| - | `last_login_at` | Data/hora do último login |
| - | `last_login_ip` | IP do cliente no último login |

## Comportamento

### Primeiro Login
- Usuário é criado na tabela `users`
- Campos de auditoria são preenchidos automaticamente
- `active` é definido como `true`
- `last_login_at` e `last_login_ip` são registrados

### Logins Subsequentes
- Dados do usuário são atualizados (email, nome, etc)
- `last_login_at` e `last_login_ip` são atualizados
- Campos de auditoria (`updated_at`, `last_modified_by`) são atualizados

### Nomes Ausentes
Se o JWT não contiver `given_name` ou `family_name`:
- `first_name` é extraído do email (parte antes do @)
- `last_name` é definido como string vazia

Exemplo:
- Email: `joao.silva@example.com`
- `first_name`: `joao.silva`
- `last_name`: ``

## Endpoints Modificados

### AuthController

#### POST /api/v1/auth/login
```java
public ResponseEntity<TokenResponse> login(
    @Valid @RequestBody LoginRequest request,
    HttpServletRequest httpRequest) // Novo parâmetro
```

#### POST /api/v1/auth/register
```java
public ResponseEntity<TokenResponse> register(
    @Valid @RequestBody RegisterRequest request,
    HttpServletRequest httpRequest) // Novo parâmetro
```

### OAuth2Controller

#### POST /api/v1/auth/oauth2/callback (NOVO)
```java
public ResponseEntity<TokenResponse> handleCallback(
    @RequestParam String code,
    @RequestParam(required = false) String redirectUri,
    HttpServletRequest request)
```

## Código de Exemplo

### Frontend - Login Normal

```typescript
async login(email: string, password: string) {
  const response = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  const data = await response.json();
  
  // Salvar token
  localStorage.setItem('access_token', data.accessToken);
  
  // Usuário já foi sincronizado automaticamente!
  // Não precisa chamar /api/v1/users/me
}
```

### Frontend - OAuth2

```typescript
// Passo 1: Obter URL de autenticação
async loginWithGoogle() {
  const response = await fetch('http://localhost:8080/api/v1/auth/oauth2/login/google');
  const data = await response.json();
  
  // Redirecionar para Google
  window.location.href = data.authUrl;
}

// Passo 2: Processar callback (na rota /auth/callback)
async handleOAuthCallback() {
  const urlParams = new URLSearchParams(window.location.search);
  const code = urlParams.get('code');
  
  if (code) {
    const response = await fetch('http://localhost:8080/api/v1/auth/oauth2/callback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        code: code,
        redirectUri: 'http://localhost:4200/auth/callback'
      })
    });
    
    const data = await response.json();
    
    // Salvar token
    localStorage.setItem('access_token', data.accessToken);
    
    // Usuário já foi sincronizado automaticamente!
    // Redirecionar para dashboard
    window.location.href = '/dashboard';
  }
}
```

## Logs

### Login Bem-Sucedido
```
INFO  - Sincronizando usuário após autenticação: user@example.com (abc-123-def)
INFO  - Criando/atualizando usuário: user@example.com
INFO  - Atualizando último login do usuário: abc-123-def
```

### OAuth2 Bem-Sucedido
```
INFO  - Sincronizando usuário após OAuth2: user@gmail.com (xyz-789-abc)
INFO  - Criando/atualizando usuário: user@gmail.com
INFO  - Atualizando último login do usuário: xyz-789-abc
```

### Erro na Sincronização
```
WARN  - Erro ao sincronizar usuário após login: [mensagem de erro]
```

**Nota:** Erros na sincronização não impedem o login. O token é retornado normalmente e o usuário pode usar a aplicação. A sincronização será tentada novamente no próximo login.

## Benefícios

1. **Transparência**: Frontend não precisa se preocupar com sincronização
2. **Automático**: Funciona para login normal e OAuth2
3. **Rastreamento**: IP e data/hora do último login são registrados automaticamente
4. **Idempotente**: Pode executar múltiplas vezes sem problemas
5. **Resiliente**: Erros na sincronização não impedem o login

## Compatibilidade

### Endpoint /api/v1/users/me

O endpoint `/api/v1/users/me` continua funcionando normalmente:

```
GET /api/v1/users/me
Authorization: Bearer <token>
```

Resposta:
```json
{
  "id": "abc-123-def",
  "keycloakId": "xyz-789",
  "email": "user@example.com",
  "firstName": "João",
  "lastName": "Silva",
  "fullName": "João Silva",
  "username": "joao.silva",
  "emailVerified": true,
  "active": true,
  "lastLoginAt": "2026-03-28T12:30:00",
  "lastLoginIp": "192.168.1.100",
  "roles": [
    {
      "id": "role-123",
      "name": "Consumer",
      "code": "CONSUMER",
      "description": "Usuário consumidor de APIs"
    }
  ],
  "createdAt": "2026-03-28T10:00:00",
  "updatedAt": "2026-03-28T12:30:00"
}
```

## Troubleshooting

### Usuário não foi criado na base de dados

1. Verificar logs do backend
2. Verificar se o token JWT está válido
3. Verificar se o JwtDecoder está configurado corretamente
4. Verificar se o UserService está funcionando

### IP não está sendo registrado

1. Verificar se o frontend está enviando o request corretamente
2. Verificar se o backend está atrás de um proxy (usar `X-Forwarded-For` header)
3. Verificar logs do backend

### Dados do usuário não estão atualizados

1. Verificar cache Redis (TTL de 20 minutos para users)
2. Limpar cache manualmente se necessário
3. Fazer logout e login novamente

## Próximos Passos

- [ ] Adicionar suporte para `X-Forwarded-For` header (proxy/load balancer)
- [ ] Adicionar métricas de sincronização
- [ ] Adicionar retry automático em caso de falha
- [ ] Adicionar webhook para notificar outros sistemas
- [ ] Adicionar sincronização de roles do Keycloak
