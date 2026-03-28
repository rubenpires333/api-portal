# 📚 Arquivos Guia - API Portal Backend

Esta pasta contém toda a documentação e guias do projeto.

## 📋 Índice de Documentos

### Módulo de Autenticação

1. **OAUTH2_SETUP.md**
   - Configuração de autenticação OAuth2
   - Integração com Google, LinkedIn, GitHub
   - Passo a passo completo

### Módulo de APIs

2. **MODULO_API_PLANEJAMENTO.md**
   - Planejamento completo do módulo
   - Estrutura de dados
   - Roadmap de implementação

3. **MODULO_API_GUIA_USO.md**
   - Guia prático de uso
   - Exemplos de requisições
   - Fluxos completos
   - Troubleshooting

4. **SISTEMA_AUDITORIA.md**
   - Sistema de auditoria automática
   - Logs de requisições HTTP
   - Cache com Redis
   - Endpoints de consulta

5. **MODULO_USER_GUIA.md**
   - Gerenciamento de usuários
   - Sistema de Roles e Permissões
   - Sincronização com Keycloak
   - Exemplos práticos

6. **DATA_INITIALIZER_GUIA.md**
   - Inicialização automática de permissões
   - Configuração de permissões customizadas
   - Criação de roles padrão
   - Troubleshooting

## 🚀 Quick Start

### 1. Configurar Ambiente

```bash
# Copiar .env.example para .env
cp .env.example .env

# Editar .env com suas configurações
# - Database local
# - Keycloak
# - Frontend URL
```

### 2. Iniciar Aplicação

```bash
# Com Maven
mvn spring-boot:run

# Ou com Docker
docker-compose up
```

### 3. Acessar Documentação

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API Docs**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/actuator/health

## 📦 Módulos Implementados

### ✅ Módulo Auth (Completo)
- Login/Registro
- JWT Authentication
- OAuth2 (Google, LinkedIn, GitHub)
- API Keys
- Atualização de perfil e senha
- Grupos e Roles

### ✅ Módulo API (Completo)
- CRUD de APIs
- Versionamento
- Endpoints
- Categorias
- Busca e filtros
- Controle de acesso por role

### ✅ Módulo Auditoria (Completo)
- Auditoria automática de entidades (createdAt, updatedAt, createdBy, lastModifiedBy)
- Logs de todas requisições HTTP
- Cache com Redis
- Endpoints de consulta (apenas SUPER_ADMIN)

### ✅ Módulo User (Completo)
- Sincronização automática com Keycloak
- Sistema de Roles e Permissões granular
- Gerenciamento de perfil de usuário
- Rastreamento de último login
- Cache com Redis
- Inicialização automática de permissões e roles

## 🔐 Autenticação

### Obter Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "senha123"
  }'
```

### Usar Token

```bash
curl -X GET http://localhost:8080/api/v1/apis/my \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

## 👥 Roles e Permissões

### SUPER_ADMIN
- Acesso total ao sistema
- Gerenciar categorias
- Aprovar APIs
- Ver estatísticas globais

### PROVIDER
- Criar e gerenciar suas APIs
- Criar versões e endpoints
- Publicar/Depreciar APIs
- Ver estatísticas das suas APIs

### CONSUMER
- Ver APIs públicas
- Buscar APIs
- Ver documentação
- Solicitar acesso a APIs privadas

## 🗄️ Banco de Dados

### Tabelas Criadas Automaticamente

**Módulo Auth:**
- `api_keys`
- `api_key_scopes`

**Módulo API:**
- `apis`
- `api_versions`
- `api_endpoints`
- `api_categories`
- `api_tags`
- `endpoint_tags`

**Módulo Auditoria:**
- `audit_logs`

**Módulo User:**
- `users`
- `roles`
- `permissions`
- `user_roles`
- `role_permissions`

## 🛠️ Tecnologias

- **Java 21**
- **Spring Boot 3.4.1**
- **PostgreSQL 16**
- **Redis 7** (Cache)
- **Keycloak 24.0**
- **Spring Security + OAuth2**
- **Spring Data JPA**
- **Swagger/OpenAPI 3**
- **Lombok**
- **DevTools** (Hot Reload)

## 📝 Convenções

### Nomenclatura de Endpoints

```
GET    /api/v1/resource          # Listar
GET    /api/v1/resource/{id}     # Obter por ID
POST   /api/v1/resource          # Criar
PUT    /api/v1/resource/{id}     # Atualizar
PATCH  /api/v1/resource/{id}     # Atualizar parcial
DELETE /api/v1/resource/{id}     # Deletar
```

### Estrutura de Resposta

```json
{
  "id": "uuid",
  "name": "string",
  "createdAt": "2026-03-28T10:00:00",
  "updatedAt": "2026-03-28T10:00:00"
}
```

### Estrutura de Erro

```json
{
  "timestamp": "2026-03-28T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensagem de erro"
}
```

## 🧪 Testes

### Swagger UI

Acesse http://localhost:8080/swagger-ui/index.html e teste todos os endpoints interativamente.

### cURL

Todos os guias contêm exemplos completos com cURL.

### Postman

Importe a collection do Swagger:
```
http://localhost:8080/api-docs
```

## 🔄 Hot Reload

O projeto usa Spring Boot DevTools:
- Salve qualquer arquivo `.java`
- Aguarde 2-3 segundos
- Mudanças aplicadas automaticamente

## 📊 Próximos Módulos

### Em Planejamento

1. **Módulo de Subscriptions**
   - Consumidores se inscrevem em APIs
   - Gerenciamento de API Keys
   - Planos e limites

2. **Módulo de Analytics**
   - Estatísticas de uso
   - Logs de requisições
   - Dashboards

3. **Módulo de Billing**
   - Cobrança por uso
   - Planos pagos
   - Integração com gateways

## 🐛 Troubleshooting

### Aplicação não inicia

1. Verifique se o PostgreSQL está rodando
2. Verifique as credenciais no `.env`
3. Verifique se a porta 8080 está livre

### Erro 401 Unauthorized

1. Verifique se o token está válido
2. Verifique se o token não expirou
3. Faça login novamente

### Swagger não carrega

1. Verifique se a aplicação está rodando
2. Acesse: http://localhost:8080/swagger-ui/index.html
3. Limpe o cache do navegador

## 📞 Suporte

Para dúvidas ou problemas:
1. Consulte os guias nesta pasta
2. Verifique o Swagger UI
3. Verifique os logs da aplicação

## 📄 Licença

Este projeto é parte do API Portal Backend.
