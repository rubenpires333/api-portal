# auth-module — API CV Platform

Módulo de autenticação e autorização da plataforma API CV.
Gere login, registo, JWT via Keycloak e validação de API Keys.

## Estrutura

```
auth-module/
├── src/main/java/com/apicv/platform/modules/auth/
│   ├── config/
│   │   ├── SecurityConfig.java          ← Spring Security + OAuth2
│   │   ├── KeycloakConfig.java          ← Admin client bean
│   │   ├── RestTemplateConfig.java      ← Bean RestTemplate
│   │   └── OpenApiConfig.java           ← Swagger + Bearer/APIKey
│   ├── controller/
│   │   └── AuthController.java          ← /api/v1/auth/**
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── RefreshRequest.java
│   │   ├── TokenResponse.java
│   │   └── AuthUserResponse.java
│   ├── exception/
│   │   ├── AuthException.java
│   │   └── AuthExceptionHandler.java
│   ├── filter/
│   │   ├── ApiKeyAuthFilter.java        ← Valida X-API-Key header
│   │   └── JwtRoleConverter.java        ← Extrai roles do JWT Keycloak
│   ├── model/
│   │   ├── UserPrincipal.java
│   │   └── enums/UserRole.java          ← SUPERADMIN | PROVIDER | CONSUMER
│   └── service/
│       ├── AuthService.java             ← login, refresh, register, me
│       ├── ApiKeyService.java           ← validação + cache Redis
│       └── KeycloakAdminService.java    ← CRUD users no Keycloak
├── src/main/resources/
│   ├── application.properties
│   └── application-docker.properties
├── src/test/
│   ├── controller/AuthControllerTest.java
│   └── service/ApiKeyServiceTest.java
├── scripts/
│   └── keycloak-setup.sh               ← Setup automático do realm
└── pom.xml
```

## Endpoints

| Método | Endpoint                        | Acesso       | Descrição                     |
|--------|---------------------------------|--------------|-------------------------------|
| POST   | /api/v1/auth/login              | Público      | Login email/password          |
| POST   | /api/v1/auth/refresh            | Público      | Renovar token                 |
| POST   | /api/v1/auth/register           | Público      | Registar PROVIDER ou CONSUMER |
| GET    | /api/v1/auth/me                 | Autenticado  | Dados do utilizador actual    |
| POST   | /api/v1/auth/verify-email/{id}  | SUPERADMIN   | Reenviar verificação          |
| POST   | /api/v1/auth/reset-password/{id}| SUPERADMIN   | Reset de password             |
| DELETE | /api/v1/auth/users/{id}         | SUPERADMIN   | Desactivar utilizador         |

## Autenticação suportada

**JWT (Bearer)** — utilizado pelo Angular e portal web
```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**API Key** — utilizado por apps/scripts que consomem APIs
```http
X-API-Key: abcdef1234567890abcdef1234567890
```

## Roles e permissões

| Role        | Keycloak Role   | Acesso na plataforma                        |
|-------------|-----------------|---------------------------------------------|
| SUPERADMIN  | platform-admin  | Tudo — aprovar providers, gerir plataforma  |
| PROVIDER    | api-provider    | Gerir as suas APIs, ver analytics           |
| CONSUMER    | api-consumer    | Subscrever e consumir APIs via API Key      |

## Setup rápido

### 1. Pré-requisitos

```bash
# Iniciar PostgreSQL, Keycloak e Redis
docker-compose up -d postgres keycloak redis
```

### 2. Configurar o Keycloak realm

```bash
chmod +x scripts/keycloak-setup.sh
./scripts/keycloak-setup.sh
```

Cria automaticamente:
- Realm `apicv`
- Client `apicv-backend` com client_secret
- Roles: `platform-admin`, `api-provider`, `api-consumer`
- Utilizador admin: `admin@apicv.cv` / `Admin@123`

### 3. Iniciar o módulo

```bash
mvn spring-boot:run
# ou com perfil docker:
SPRING_PROFILES_ACTIVE=docker mvn spring-boot:run
```

### 4. Testar

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@apicv.cv","password":"Admin@123"}'

# Ver utilizador autenticado
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <access_token>"

# Swagger
open http://localhost:8080/swagger-ui.html
```

## Variáveis de ambiente

| Variável                    | Default                        | Descrição                    |
|-----------------------------|--------------------------------|------------------------------|
| KEYCLOAK_URL                | http://localhost:8180          | URL do Keycloak              |
| KEYCLOAK_REALM              | apicv                          | Nome do realm                |
| KEYCLOAK_CLIENT_ID          | apicv-backend                  | Client ID                    |
| KEYCLOAK_CLIENT_SECRET      | change-me-in-production        | Client secret                |
| SPRING_DATASOURCE_URL       | jdbc:postgresql://localhost... | URL da base de dados         |
| REDIS_HOST                  | localhost                      | Host do Redis                |
| REDIS_PORT                  | 6379                           | Porta do Redis               |

## Próximo módulo

Após este módulo estar funcional, avançar para o **user-module**:
- Entidade `User` sincronizada com Keycloak
- Entidade `Provider` com perfil detalhado
- Aprovação de providers pelo SUPERADMIN
