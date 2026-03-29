# Arquitetura do Marketplace de APIs

## ESTRUTURA DE TABELAS E SUAS FUNÇÕES

### 1. **apis** - Tabela Principal
**Função:** Armazena as informações gerais da API que aparecem no marketplace

**Campos Principais:**
- `name` - Nome da API exibido no marketplace
- `slug` - Identificador único usado na URL do gateway
- `short_desc` - Descrição curta (card do marketplace)
- `description` - Descrição completa (página de detalhes)
- `category` - Categoria da API (Pagamentos, Logística, etc.)
- `tags` - Array de tags para busca e filtros
- `status` - DRAFT, ACTIVE, DEPRECATED, INACTIVE
- `visibility` - PUBLIC, PRIVATE
- `base_url` - URL real da API do provider
- `documentation_url` - Link para documentação técnica
- `terms_of_service_url` - Link para termos de uso
- `auth_type` - Tipo de autenticação (NONE, API_KEY, BEARER, OAUTH2, BASIC)
- `logo_url` - Logo da API (marketplace)
- `rate_limit` - Limite de requisições
- `provider_id` - Quem criou a API

**Quando usar:** Ao criar/editar uma API no módulo Provider

---

### 2. **api_versions** - Versionamento
**Função:** Gerencia diferentes versões da mesma API (v1.0, v2.0, v3.0)

**Campos Principais:**
- `api_id` - Referência à API pai
- `version` - Número da versão (ex: "1.0.0", "2.0.0")
- `description` - Changelog ou descrição da versão
- `status` - DRAFT, ACTIVE, DEPRECATED
- `is_default` - Se é a versão padrão
- `is_deprecated` - Se está depreciada
- `base_url` - URL específica desta versão (opcional)
- `openapi_spec` - Especificação OpenAPI em JSON

**Quando usar:** 
- Após criar a API, adicionar versão inicial (v1.0.0)
- Ao lançar novas versões mantendo compatibilidade
- Depreciar versões antigas

---

### 3. **api_endpoints** - Endpoints da API
**Função:** Define os endpoints específicos de cada versão (GET /users, POST /orders)

**Campos Principais:**
- `version_id` - Referência à versão da API
- `path` - Caminho do endpoint (ex: "/users", "/orders/{id}")
- `method` - Método HTTP (GET, POST, PUT, DELETE, PATCH)
- `summary` - Resumo do que o endpoint faz
- `description` - Descrição detalhada
- `tags` - Tags específicas do endpoint
- `requires_auth` - Se requer autenticação
- `is_deprecated` - Se está depreciado
- `request_example` - Exemplo de requisição
- `response_example` - Exemplo de resposta

**Quando usar:**
- Após criar versão, adicionar endpoints disponíveis
- Documentar cada operação da API
- Permitir teste no sandbox

---

### 4. **api_tags** - Tags da API
**Função:** Armazena tags da API para busca e categorização

**Estrutura:** ElementCollection (array) na entidade Api
- Não é tabela separada, é uma collection table do JPA
- Permite múltiplas tags por API
- Usado para filtros e busca no marketplace

---

### 5. **endpoint_tags** - Tags dos Endpoints
**Função:** Agrupa endpoints por funcionalidade (users, payments, orders)

**Estrutura:** ElementCollection (array) na entidade ApiEndpoint
- Organiza endpoints por domínio
- Facilita navegação na documentação
- Usado no sandbox para agrupar testes

---

### 6. **api_key** (subscriptions)
**Função:** Chaves de acesso dos consumers às APIs

**Quando usar:**
- Consumer se inscreve em uma API
- Sistema gera API key única
- Usada para autenticação e rate limiting

---

## FLUXO COMPLETO DO MARKETPLACE

### FASE 1: Provider Cria API (ATUAL)
```
1. Provider preenche formulário:
   ✓ Nome, categoria, descrição
   ✓ URL base, autenticação
   ✓ Documentação, termos de uso
   ✓ Logo, tags, rate limit
   
2. Sistema cria registro na tabela "apis"
3. Status inicial: DRAFT
```

### FASE 2: Provider Adiciona Versão (PRÓXIMO)
```
1. Provider acessa API criada
2. Clica em "Adicionar Versão"
3. Preenche: versão (1.0.0), changelog
4. Sistema cria registro em "api_versions"
5. Marca como versão padrão
```

### FASE 3: Provider Adiciona Endpoints (PRÓXIMO)
```
1. Provider acessa versão criada
2. Clica em "Adicionar Endpoint"
3. Preenche:
   - Método HTTP (GET, POST, etc.)
   - Path (/users, /orders/{id})
   - Descrição
   - Exemplos de request/response
4. Sistema cria registro em "api_endpoints"
```

### FASE 4: Provider Publica API
```
1. Provider revisa tudo
2. Clica em "Publicar"
3. Status muda: DRAFT → ACTIVE
4. API aparece no marketplace para consumers
```

### FASE 5: Consumer Usa API
```
1. Consumer vê API no marketplace
2. Se inscreve (gera API key)
3. Faz requisição ao gateway:
   GET http://localhost:8080/gateway/api/viacep/ws/01001000/json
   
4. Gateway:
   - Busca configuração da API pelo slug
   - Valida API key do consumer
   - Aplica rate limiting
   - Faz proxy para URL real do provider
   - Retorna resposta ao consumer
```

---

## CAMPOS ADICIONADOS NO FORMULÁRIO

✅ **documentationUrl** - URL da documentação técnica
✅ **termsOfServiceUrl** - URL dos termos de uso
✅ **logoUrl** - Logo da API (preview no card)
✅ **iconUrl** - Ícone da API
✅ **tags** - Sistema de adicionar/remover tags
✅ **rateLimit** - Limite de requisições
✅ **rateLimitPeriod** - Período (second, minute, hour, day)
✅ **requiresApproval** - Se requer aprovação manual

---

## PRÓXIMOS PASSOS

1. **Gerenciamento de Versões** - CRUD de versões da API
2. **Gerenciamento de Endpoints** - CRUD de endpoints por versão
3. **Sandbox/Teste** - Interface para testar endpoints
4. **Publicação** - Workflow de DRAFT → ACTIVE
5. **Métricas** - Dashboard com uso, latência, uptime

---

## OBSERVAÇÕES IMPORTANTES

- **Métodos HTTP** não são da API, são dos **endpoints**
- Cada API pode ter múltiplas **versões**
- Cada versão pode ter múltiplos **endpoints**
- Cada endpoint tem seu **método** (GET, POST, PUT, DELETE, PATCH)
- O gateway usa o **slug** para identificar a API
- O **baseUrl** é a URL real do provider (nunca exposta ao consumer)


---

## ARQUITETURA DE AUTENTICAÇÃO

### DOIS NÍVEIS DE AUTENTICAÇÃO

#### 1. **API Key (tabela `api_key`)** - Token do CONSUMER
**Função:** Autenticar o consumer no Gateway do API Portal

**Fluxo:**
- Consumer se inscreve em uma API no marketplace
- Sistema gera um token único (API Key)
- Consumer usa este token para fazer requisições ao Gateway
- Gateway valida o token antes de fazer proxy

**Exemplo:**
```http
GET http://localhost:8080/gateway/api/viacep/ws/01001000/json
X-API-Key: abc123xyz789  ← Token do Consumer (validado pelo Gateway)
```

---

#### 2. **Endpoint Auth Config (tabela `api_endpoints`)** - Credenciais do PROVIDER
**Função:** Credenciais que o Gateway usa para autenticar na API do Provider

**Campos:**
- `auth_headers_json` - Headers de autenticação (JSON)
- `auth_query_params_json` - Query params de autenticação (JSON)

**Quando usar:**
- Provider define como o Gateway deve se autenticar na API dele
- Gateway usa essas credenciais ao fazer proxy
- Credenciais ficam ocultas do consumer

**Exemplo:**
Provider configura:
```json
{
  "authHeaders": [
    {"key": "Authorization", "value": "Bearer xyz123abc"}
  ],
  "authQueryParams": [
    {"key": "api_key", "value": "provider-secret-key"}
  ]
}
```

Gateway faz proxy:
```http
GET https://api.provider.com/v1/users
Authorization: Bearer xyz123abc  ← Credencial do Provider
```

---

### FLUXO COMPLETO DE AUTENTICAÇÃO

```
1. Consumer faz requisição ao Gateway
   ↓
   GET /gateway/api/viacep/ws/01001000/json
   X-API-Key: consumer-token-123  ← Token do Consumer
   
2. Gateway valida token do Consumer
   ↓
   - Busca na tabela api_key
   - Verifica se está ativo
   - Aplica rate limiting
   
3. Gateway busca configuração do endpoint
   ↓
   - Busca API pelo slug "viacep"
   - Busca versão padrão
   - Busca endpoint GET /ws/{cep}/json
   - Lê auth_headers_json e auth_query_params_json
   
4. Gateway faz proxy para API do Provider
   ↓
   GET https://viacep.com.br/ws/01001000/json
   Authorization: Bearer provider-secret  ← Credencial do Provider
   
5. Gateway retorna resposta ao Consumer
```

---

### DIFERENÇA ENTRE AS TABELAS

| Aspecto | api_key | api_endpoints (auth fields) |
|---------|---------|----------------------------|
| **Quem define** | Sistema (auto-gerado) | Provider (manual) |
| **Quem usa** | Consumer | Gateway |
| **Valida quem** | Consumer no Gateway | Gateway no Provider |
| **Onde fica** | Tabela separada | Campos JSON no endpoint |
| **Quando cria** | Consumer se inscreve | Provider configura endpoint |
| **Visível para** | Consumer (seu próprio token) | Apenas Gateway (oculto) |
