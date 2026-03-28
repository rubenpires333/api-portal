# Guia de Uso - Módulo API

## 🎯 Visão Geral

O Módulo API permite que provedores registrem suas APIs e consumidores as descubram e utilizem.

## 📋 Estrutura Implementada

✅ **Entidades**
- Api (API principal)
- ApiVersion (Versões)
- ApiEndpoint (Endpoints)
- ApiCategory (Categorias)

✅ **Enums**
- ApiStatus (DRAFT, PUBLISHED, DEPRECATED, ARCHIVED)
- ApiVisibility (PUBLIC, PRIVATE, INTERNAL)
- AuthType (NONE, API_KEY, OAUTH2, BASIC, BEARER)

✅ **Repositories** com queries customizadas
✅ **Services** com lógica de negócio
✅ **Controllers** com endpoints REST
✅ **DTOs** para Request/Response
✅ **Exception Handling**

## 🔐 Permissões

### SUPER_ADMIN
- Todas as operações
- Gerenciar categorias
- Aprovar/Rejeitar APIs

### PROVIDER
- Criar e gerenciar suas próprias APIs
- Criar versões e endpoints
- Publicar/Depreciar suas APIs

### CONSUMER
- Ver APIs públicas
- Buscar APIs
- Ver documentação

## 📡 Endpoints Disponíveis

### 1. Categorias

```http
# Listar categorias (público)
GET /api/v1/categories

# Obter categoria
GET /api/v1/categories/{id}

# Criar categoria (ADMIN)
POST /api/v1/categories
Authorization: Bearer TOKEN

# Atualizar categoria (ADMIN)
PUT /api/v1/categories/{id}
Authorization: Bearer TOKEN

# Deletar categoria (ADMIN)
DELETE /api/v1/categories/{id}
Authorization: Bearer TOKEN
```

### 2. APIs

```http
# Listar APIs públicas
GET /api/v1/apis?page=0&size=20

# Buscar APIs
GET /api/v1/apis/search?q=pagamento

# Obter API por ID
GET /api/v1/apis/{id}

# Obter API por slug
GET /api/v1/apis/slug/minha-api

# Minhas APIs (PROVIDER)
GET /api/v1/apis/my
Authorization: Bearer TOKEN

# Criar API (PROVIDER)
POST /api/v1/apis
Authorization: Bearer TOKEN

# Atualizar API (PROVIDER)
PUT /api/v1/apis/{id}
Authorization: Bearer TOKEN

# Publicar API (PROVIDER)
PATCH /api/v1/apis/{id}/publish
Authorization: Bearer TOKEN

# Depreciar API (PROVIDER)
PATCH /api/v1/apis/{id}/deprecate
Authorization: Bearer TOKEN

# Deletar API (PROVIDER)
DELETE /api/v1/apis/{id}
Authorization: Bearer TOKEN
```

### 3. Versões

```http
# Listar versões
GET /api/v1/apis/{apiId}/versions

# Obter versão
GET /api/v1/apis/{apiId}/versions/{id}

# Criar versão (PROVIDER)
POST /api/v1/apis/{apiId}/versions
Authorization: Bearer TOKEN

# Definir versão padrão (PROVIDER)
PATCH /api/v1/apis/{apiId}/versions/{id}/default
Authorization: Bearer TOKEN

# Depreciar versão (PROVIDER)
PATCH /api/v1/apis/{apiId}/versions/{id}/deprecate?message=Mensagem
Authorization: Bearer TOKEN

# Deletar versão (PROVIDER)
DELETE /api/v1/apis/{apiId}/versions/{id}
Authorization: Bearer TOKEN
```

### 4. Endpoints

```http
# Listar endpoints
GET /api/v1/versions/{versionId}/endpoints

# Obter endpoint
GET /api/v1/versions/{versionId}/endpoints/{id}

# Criar endpoint (PROVIDER)
POST /api/v1/versions/{versionId}/endpoints
Authorization: Bearer TOKEN

# Atualizar endpoint (PROVIDER)
PUT /api/v1/versions/{versionId}/endpoints/{id}
Authorization: Bearer TOKEN

# Deletar endpoint (PROVIDER)
DELETE /api/v1/versions/{versionId}/endpoints/{id}
Authorization: Bearer TOKEN
```

## 📝 Exemplos de Uso

### 1. Criar Categoria (ADMIN)

```bash
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pagamentos",
    "description": "APIs de processamento de pagamentos",
    "iconUrl": "https://example.com/icon.png",
    "displayOrder": 1
  }'
```

### 2. Criar API (PROVIDER)

```bash
curl -X POST http://localhost:8080/api/v1/apis \
  -H "Authorization: Bearer TOKEN_PROVIDER" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "API de Pagamentos",
    "shortDescription": "Processe pagamentos de forma segura",
    "description": "API completa para processamento de pagamentos com suporte a múltiplos métodos",
    "categoryId": "uuid-da-categoria",
    "visibility": "PUBLIC",
    "baseUrl": "https://api.exemplo.com",
    "documentationUrl": "https://docs.exemplo.com",
    "authType": "API_KEY",
    "tags": ["pagamento", "fintech", "pix"],
    "rateLimit": 1000,
    "rateLimitPeriod": "hour",
    "requiresApproval": false
  }'
```

### 3. Criar Versão

```bash
curl -X POST http://localhost:8080/api/v1/apis/{apiId}/versions \
  -H "Authorization: Bearer TOKEN_PROVIDER" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1",
    "description": "Primeira versão da API",
    "isDefault": true,
    "baseUrl": "https://api.exemplo.com/v1"
  }'
```

### 4. Criar Endpoint

```bash
curl -X POST http://localhost:8080/api/v1/versions/{versionId}/endpoints \
  -H "Authorization: Bearer TOKEN_PROVIDER" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/payments",
    "method": "POST",
    "summary": "Criar pagamento",
    "description": "Cria um novo pagamento",
    "tags": ["pagamento"],
    "requiresAuth": true,
    "requestExample": "{\"amount\": 100.00, \"currency\": \"BRL\"}",
    "responseExample": "{\"id\": \"123\", \"status\": \"pending\"}"
  }'
```

### 5. Publicar API

```bash
curl -X PATCH http://localhost:8080/api/v1/apis/{id}/publish \
  -H "Authorization: Bearer TOKEN_PROVIDER"
```

### 6. Buscar APIs

```bash
# Buscar por texto
curl http://localhost:8080/api/v1/apis/search?q=pagamento

# Listar todas (paginado)
curl http://localhost:8080/api/v1/apis?page=0&size=20&sort=name,asc
```

## 🔄 Fluxo Completo

### Para PROVIDER

1. **Login**
   ```bash
   POST /api/v1/auth/login
   ```

2. **Criar API**
   ```bash
   POST /api/v1/apis
   ```

3. **Criar Versão**
   ```bash
   POST /api/v1/apis/{apiId}/versions
   ```

4. **Criar Endpoints**
   ```bash
   POST /api/v1/versions/{versionId}/endpoints
   ```

5. **Publicar API**
   ```bash
   PATCH /api/v1/apis/{id}/publish
   ```

### Para CONSUMER

1. **Buscar APIs**
   ```bash
   GET /api/v1/apis/search?q=termo
   ```

2. **Ver Detalhes**
   ```bash
   GET /api/v1/apis/{id}
   ```

3. **Ver Versões**
   ```bash
   GET /api/v1/apis/{apiId}/versions
   ```

4. **Ver Endpoints**
   ```bash
   GET /api/v1/versions/{versionId}/endpoints
   ```

## 🗄️ Estrutura do Banco de Dados

As tabelas serão criadas automaticamente pelo Hibernate:

- `apis` - APIs principais
- `api_versions` - Versões das APIs
- `api_endpoints` - Endpoints
- `api_categories` - Categorias
- `api_tags` - Tags das APIs
- `endpoint_tags` - Tags dos endpoints

## ⚙️ Configuração

Nenhuma configuração adicional necessária! O módulo usa:
- Mesma configuração de banco do módulo Auth
- Mesma configuração de segurança
- DevTools para hot reload

## 🧪 Testar no Swagger

Acesse: http://localhost:8080/swagger-ui/index.html

Você verá as novas seções:
- **APIs** - Gerenciamento de APIs
- **API Versions** - Versões
- **API Endpoints** - Endpoints
- **API Categories** - Categorias

## 📊 Próximas Features (Futuro)

- [ ] Importar OpenAPI/Swagger spec
- [ ] Sistema de aprovação de APIs
- [ ] Estatísticas de uso
- [ ] Rate limiting real
- [ ] Sandbox para testar endpoints
- [ ] Webhooks
- [ ] Monitoramento de uptime

## 🐛 Troubleshooting

### Erro: "Categoria não encontrada"
- Crie uma categoria primeiro (como ADMIN)

### Erro: "Você não tem permissão"
- Verifique se está autenticado
- Verifique se tem a role correta (PROVIDER/SUPER_ADMIN)

### Erro: "API com este nome já existe"
- O slug é gerado automaticamente do nome
- Use um nome diferente

## 📚 Recursos Adicionais

- Documentação Swagger: http://localhost:8080/swagger-ui/index.html
- Health Check: http://localhost:8080/actuator/health
