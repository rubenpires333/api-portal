# API Portal Backend

Backend da plataforma de gestão de APIs desenvolvido com Spring Boot 3.4.1 e Java 21.

## 🚀 Quick Start

**Primeira vez?** Veja o guia rápido: [QUICK_START.md](QUICK_START.md)

**Desenvolvimento diário:**
```bash
./dev.sh start && ./dev.sh run  # Linux/Mac
.\dev.bat start && .\dev.bat run    # Windows (PowerShell)
```

## Tecnologias

- Java 21
- Spring Boot 3.4.1
- Spring Security + OAuth2
- PostgreSQL 16
- Keycloak 24.0
- Docker & Docker Compose
- Swagger/OpenAPI 3.0
- Lombok
- Maven

## Estrutura do Projeto

```
api-portal-backend/
├── src/main/java/com/api_portal/backend/
│   ├── modules/
│   │   └── auth/              ✅ Módulo de autenticação (implementado)
│   │       ├── config/        → Configurações de segurança e Swagger
│   │       ├── controller/    → Endpoints REST
│   │       ├── dto/           → Request/Response objects
│   │       ├── exception/     → Exception handlers
│   │       ├── model/         → Entidades e enums
│   │       └── service/       → Lógica de negócio
│   ├── config/                → Configurações globais
│   ├── shared/                → Utilitários compartilhados
│   └── web/                   → Configurações web
├── docker-compose.yml         → Orquestração de containers
├── Dockerfile                 → Build da aplicação
├── .env                       → Variáveis de ambiente (não versionado)
└── .env.example               → Template de variáveis
```

## Início Rápido

### Opção 1: Desenvolvimento Local (Recomendado - Mais Rápido) ⚡

Execute apenas a infraestrutura no Docker e a aplicação localmente:

```bash
# Linux/Mac
./dev.sh full

# Windows
dev.bat full
```

Ou manualmente:

```bash
# 1. Iniciar infraestrutura
docker-compose up -d postgres keycloak

# 2. Aguardar e configurar Keycloak (1-2 minutos)
chmod +x scripts/keycloak-setup.sh
./scripts/keycloak-setup.sh

# 3. Executar aplicação localmente
./mvnw spring-boot:run  # Linux/Mac
mvnw.cmd spring-boot:run  # Windows
```

**Vantagens:**
- ✅ Compilação incremental rápida (~10-30 segundos)
- ✅ Hot reload com Spring DevTools
- ✅ Não precisa rebuild do Docker a cada mudança

### Opção 2: Tudo no Docker (Mais Lento)

```bash
# Iniciar tudo (primeira vez ou após mudanças)
docker-compose up --build

# Apenas iniciar (sem rebuild)
docker-compose up
```

**Desvantagens:**
- ❌ Rebuild baixa todas as dependências (~5-10 minutos)
- ❌ Sem hot reload
- ❌ Lento para desenvolvimento

### Pré-requisitos

**Para Opção 1 (Recomendado):**
- Docker & Docker Compose
- Java 21
- Maven (ou use o Maven Wrapper incluído: `./mvnw` no Linux/Mac ou `mvnw.cmd` no Windows)

**Nota para Windows:** No PowerShell, use `.\dev.bat` em vez de `dev.bat`

**Para Opção 2:**
- Apenas Docker & Docker Compose

### 6. Acessar aplicação

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Keycloak Admin: http://localhost:8180 (admin/admin123)

## 🛠️ Scripts Helper

Para facilitar o desenvolvimento, use os scripts incluídos:

### Linux/Mac
```bash
./dev.sh start    # Inicia infraestrutura
./dev.sh setup    # Configura Keycloak
./dev.sh run      # Executa aplicação
./dev.sh status   # Ver status dos serviços
./dev.sh logs     # Ver logs
./dev.sh help     # Ver todos os comandos
```

### Windows
```cmd
.\dev.bat start     # Inicia infraestrutura
.\dev.bat run       # Executa aplicação
.\dev.bat status    # Ver status dos serviços
.\dev.bat logs      # Ver logs
.\dev.bat help      # Ver todos os comandos
```

Ver guia completo: [DESENVOLVIMENTO_LOCAL.md](DESENVOLVIMENTO_LOCAL.md)

## Módulos Implementados

### ✅ Auth Module

Autenticação e autorização via Keycloak.

**Endpoints:**
- `POST /api/v1/auth/login` - Login com email/password
- `GET /api/v1/auth/health` - Health check

**Credenciais padrão:**
- Email: admin@apicv.cv
- Password: Admin@123

Ver documentação completa: [TESTE_AUTH.md](TESTE_AUTH.md)

## Testes

```bash
# Compilar
mvn clean compile

# Executar testes
mvn test

# Executar com cobertura
mvn test jacoco:report
```

## Swagger/OpenAPI

A documentação da API está disponível em:
- JSON: http://localhost:8080/api-docs
- UI: http://localhost:8080/swagger-ui.html

Todos os endpoints estão documentados com:
- Descrições detalhadas
- Exemplos de request/response
- Códigos de status HTTP
- Suporte para autenticação Bearer

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| SPRING_DATASOURCE_URL | jdbc:postgresql://localhost:5432/apiportal | URL do banco |
| SPRING_DATASOURCE_USERNAME | apiportal | Usuário do banco |
| SPRING_DATASOURCE_PASSWORD | apiportal123 | Senha do banco |
| KEYCLOAK_URL | http://localhost:8180 | URL do Keycloak |
| KEYCLOAK_REALM | apicv | Realm do Keycloak |
| KEYCLOAK_CLIENT_ID | apicv-backend | Client ID |
| KEYCLOAK_CLIENT_SECRET | change-me-in-production | Client Secret |

## Próximos Módulos

1. **User Module** - Gestão de utilizadores e providers
2. **API Module** - Gestão de APIs e endpoints
3. **Subscription Module** - Subscrições e API Keys
4. **Analytics Module** - Métricas e estatísticas
5. **Notification Module** - Emails e notificações

## Comandos Úteis

```bash
# Parar todos os containers
docker-compose down

# Limpar volumes (reset completo)
docker-compose down -v

# Ver logs
docker-compose logs -f backend

# Rebuild da aplicação
mvn clean package -DskipTests
docker-compose up --build backend

# Acessar banco de dados
docker exec -it apiportal-postgres psql -U apiportal -d apiportal
```

## Contribuir

1. Siga as diretrizes em `Diretrizes/README.md`
2. Mantenha o código limpo e documentado
3. Adicione testes para novas funcionalidades
4. Atualize a documentação Swagger

## Licença

Propriedade de API CV Platform
