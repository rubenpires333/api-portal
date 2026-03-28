# Módulo de Autenticação - Completo

Módulo completo de autenticação integrado com Keycloak, suportando JWT e API Keys.

## Funcionalidades Implementadas

✅ Login com email/password (JWT)  
✅ Refresh token  
✅ Registro de utilizadores (PROVIDER/CONSUMER)  
✅ Endpoint /me para dados do utilizador autenticado  
✅ Validação JWT em endpoints protegidos  
✅ Gestão completa de API Keys  
✅ Autenticação via API Key (header X-API-Key)  
✅ Documentação Swagger completa  
✅ Exception handling global  
✅ Validação de dados  

## Endpoints Disponíveis

### Autenticação (Públicos)

#### POST /api/v1/auth/login
Autentica um utilizador e retorna tokens JWT.

**Request:**
```json
{
  "email": "admin@apicv.cv",
  "password": "Admin@123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

#### POST /api/v1/auth/refresh
Renova o access token usando o refresh token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

#### POST /api/v1/auth/register
Cria um novo utilizador na plataforma.

**Request:**
```json
{
  "name": "João Silva",
  "email": "joao@example.com",
  "password": "Password@123",
  "role": "CONSUMER"
}
```

Roles: `SUPERADMIN`, `PROVIDER`, `CONSUMER`

#### GET /api/v1/auth/health
Health check do módulo.

### Utilizador Autenticado (Requer JWT)

#### GET /api/v1/auth/me
Retorna dados do utilizador autenticado.

**Headers:**
```
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Admin User",
  "email": "admin@apicv.cv",
  "emailVerified": true,
  "roles": ["platform-admin", "offline_access"]
}
```

### API Keys (Requer JWT)

#### POST /api/v1/api-keys
Cria uma nova API Key.

**Request:**
```json
{
  "name": "Produção - App Mobile",
  "description": "Chave para acesso da aplicação mobile",
  "expiresInDays": 365
}
```

**Response:**
```json
{
  "id": 1,
  "keyValue": "abc123def456...",
  "name": "Produção - App Mobile",
  "description": "Chave para acesso da aplicação mobile",
  "active": true,
  "createdAt": "2026-03-27T18:30:00",
  "expiresAt": "2027-03-27T18:30:00",
  "lastUsedAt": null
}
```

⚠️ O `keyValue` só é retornado na criação!

#### GET /api/v1/api-keys
Lista todas as API Keys do utilizador.

#### DELETE /api/v1/api-keys/{id}
Revoga (desativa) uma API Key.

## Autenticação

O módulo suporta dois métodos de autenticação:

### 1. JWT Bearer Token
```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

Usado por aplicações web e mobile que fazem login de utilizadores.

### 2. API Key
```http
X-API-Key: abc123def456...
```

Usado por aplicações e scripts que precisam de acesso programático.

## Configuração

As credenciais estão no arquivo `.env` na raiz do projeto:

```env
# Keycloak (do docker-compose.yml)
KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=apicv
KEYCLOAK_CLIENT_ID=apicv-backend
KEYCLOAK_CLIENT_SECRET=change-me-in-production
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
```

## Swagger

Acesse a documentação interativa em:
- http://localhost:8080/swagger-ui.html

A documentação inclui:
- Todos os endpoints com exemplos
- Suporte para testar com Bearer Token
- Suporte para testar com API Key
- Schemas de request/response

## Estrutura do Código

```
auth/
├── config/
│   ├── SecurityConfig.java          → Spring Security + JWT + API Key
│   ├── OpenApiConfig.java           → Swagger com Bearer e API Key
│   └── RestTemplateConfig.java      → Bean RestTemplate
├── controller/
│   ├── AuthController.java          → Endpoints de autenticação
│   └── ApiKeyController.java        → Endpoints de API Keys
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── RefreshRequest.java
│   ├── TokenResponse.java
│   ├── AuthUserResponse.java
│   ├── ApiKeyRequest.java
│   └── ApiKeyResponse.java
├── exception/
│   ├── AuthException.java
│   └── AuthExceptionHandler.java    → Global exception handler
├── filter/
│   └── ApiKeyAuthFilter.java        → Valida X-API-Key header
├── model/
│   ├── ApiKey.java                  → Entidade JPA
│   └── enums/UserRole.java
├── repository/
│   └── ApiKeyRepository.java        → JPA Repository
└── service/
    ├── AuthService.java             → Login, refresh, register, me
    ├── ApiKeyService.java           → CRUD de API Keys
    └── KeycloakAdminService.java    → Gestão de users no Keycloak
```

## Testes

Ver guia completo de testes: [TESTE_AUTH.md](../../../TESTE_AUTH.md)

## Segurança

- Tokens JWT validados via Keycloak JWKS
- API Keys armazenadas de forma segura no banco
- Geração de API Keys com SecureRandom
- Validação de expiração de tokens e keys
- Exception handling sem expor detalhes internos
- Validação de dados com Bean Validation

## Próximos Passos

O módulo auth está completo e pronto para uso. Próximos módulos a implementar:

1. **User Module** - Gestão completa de utilizadores e providers
2. **API Module** - Gestão de APIs e endpoints
3. **Subscription Module** - Subscrições avançadas
4. **Analytics Module** - Métricas de uso
5. **Notification Module** - Emails e notificações
