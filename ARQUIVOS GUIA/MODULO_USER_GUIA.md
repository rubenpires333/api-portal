# Módulo User - Guia de Uso

## Visão Geral

O módulo User gerencia usuários, roles e permissões do sistema. Ele sincroniza automaticamente os dados dos usuários do Keycloak para o banco de dados local, permitindo gerenciamento avançado de permissões e auditoria.

## Características

- Sincronização automática com Keycloak
- Sistema de Roles e Permissões granular
- Auditoria completa (createdAt, updatedAt, createdBy, lastModifiedBy)
- Cache com Redis
- Rastreamento de último login
- Perfil de usuário customizável

## Entidades

### User
- Dados básicos: email, nome, username
- Informações de perfil: bio, company, location, website
- Controle: active, emailVerified
- Auditoria: lastLoginAt, lastLoginIp
- Relacionamento N:N com Roles

### Role
- Nome e código único
- Descrição
- Flag isSystem (roles do sistema não podem ser deletadas)
- Relacionamento N:N com Permissions
- Relacionamento N:N com Users

### Permission
- Nome e código único
- Resource (ex: "api", "user", "category")
- Action (ex: "create", "read", "update", "delete")
- Relacionamento N:N com Roles

## Fluxo de Sincronização

### 1. Primeiro Login

Quando um usuário faz login pela primeira vez:

```
1. Usuário autentica no Keycloak
2. Backend recebe JWT token
3. Endpoint /api/v1/users/me é chamado
4. KeycloakSyncService extrai dados do token
5. UserService cria novo usuário no banco
6. Último login é registrado
7. Dados do usuário são retornados
```

### 2. Logins Subsequentes

```
1. Usuário autentica no Keycloak
2. Backend recebe JWT token
3. Endpoint /api/v1/users/me é chamado
4. KeycloakSyncService extrai dados do token
5. UserService atualiza dados do usuário (se mudaram)
6. Último login é atualizado
7. Dados do usuário são retornados
```

## Endpoints

### Usuários

#### Obter Dados do Usuário Autenticado

```http
GET /api/v1/users/me
Authorization: Bearer {token}
```

Resposta:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "keycloakId": "2ac41dbe-2781-4305-9700-51c92e55a70a",
  "email": "user@example.com",
  "firstName": "João",
  "lastName": "Silva",
  "fullName": "João Silva",
  "username": "joao.silva",
  "phoneNumber": "+238 999 9999",
  "avatarUrl": "https://example.com/avatar.jpg",
  "emailVerified": true,
  "active": true,
  "lastLoginAt": "2026-03-28T10:30:00",
  "lastLoginIp": "192.168.1.100",
  "roles": [
    {
      "id": "role-uuid",
      "name": "Provider",
      "code": "PROVIDER",
      "description": "Provedor de APIs"
    }
  ],
  "bio": "Desenvolvedor Full Stack",
  "company": "Tech Company",
  "location": "Praia, Cabo Verde",
  "website": "https://example.com",
  "createdAt": "2026-03-28T10:00:00",
  "updatedAt": "2026-03-28T10:30:00"
}
```

#### Atualizar Perfil do Usuário Autenticado

```http
PUT /api/v1/users/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "firstName": "João",
  "lastName": "Silva",
  "phoneNumber": "+238 999 9999",
  "avatarUrl": "https://example.com/avatar.jpg",
  "bio": "Desenvolvedor Full Stack",
  "company": "Tech Company",
  "location": "Praia, Cabo Verde",
  "website": "https://example.com"
}
```

#### Listar Todos os Usuários (SUPER_ADMIN)

```http
GET /api/v1/users?page=0&size=20
Authorization: Bearer {token}
```

#### Buscar Usuários (SUPER_ADMIN)

```http
GET /api/v1/users/search?query=joão&page=0&size=20
Authorization: Bearer {token}
```

#### Obter Usuário por ID (SUPER_ADMIN)

```http
GET /api/v1/users/{id}
Authorization: Bearer {token}
```

#### Atribuir Roles ao Usuário (SUPER_ADMIN)

```http
POST /api/v1/users/{id}/roles
Authorization: Bearer {token}
Content-Type: application/json

["role-uuid-1", "role-uuid-2"]
```

#### Desativar Usuário (SUPER_ADMIN)

```http
POST /api/v1/users/{id}/deactivate
Authorization: Bearer {token}
```

#### Ativar Usuário (SUPER_ADMIN)

```http
POST /api/v1/users/{id}/activate
Authorization: Bearer {token}
```

#### Listar Usuários por Role (SUPER_ADMIN)

```http
GET /api/v1/users/role/PROVIDER?page=0&size=20
Authorization: Bearer {token}
```

### Roles

#### Criar Role (SUPER_ADMIN)

```http
POST /api/v1/roles
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "API Manager",
  "code": "API_MANAGER",
  "description": "Gerencia APIs do sistema",
  "permissionIds": [
    "perm-uuid-1",
    "perm-uuid-2"
  ]
}
```

#### Listar Todas as Roles (SUPER_ADMIN)

```http
GET /api/v1/roles
Authorization: Bearer {token}
```

Resposta:
```json
[
  {
    "id": "role-uuid",
    "name": "API Manager",
    "code": "API_MANAGER",
    "description": "Gerencia APIs do sistema",
    "isSystem": false,
    "active": true,
    "permissions": [
      {
        "id": "perm-uuid",
        "name": "Criar API",
        "code": "api.create",
        "resource": "api",
        "action": "create"
      }
    ],
    "userCount": 5,
    "createdAt": "2026-03-28T10:00:00",
    "updatedAt": "2026-03-28T10:00:00"
  }
]
```

#### Obter Role por ID (SUPER_ADMIN)

```http
GET /api/v1/roles/{id}
Authorization: Bearer {token}
```

#### Atualizar Role (SUPER_ADMIN)

```http
PUT /api/v1/roles/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "API Manager",
  "code": "API_MANAGER",
  "description": "Gerencia APIs do sistema",
  "permissionIds": [
    "perm-uuid-1",
    "perm-uuid-2",
    "perm-uuid-3"
  ]
}
```

#### Deletar Role (SUPER_ADMIN)

```http
DELETE /api/v1/roles/{id}
Authorization: Bearer {token}
```

### Permissões

#### Criar Permissão (SUPER_ADMIN)

```http
POST /api/v1/permissions
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Criar API",
  "code": "api.create",
  "description": "Permite criar novas APIs",
  "resource": "api",
  "action": "create"
}
```

#### Listar Todas as Permissões (SUPER_ADMIN)

```http
GET /api/v1/permissions
Authorization: Bearer {token}
```

Resposta:
```json
[
  {
    "id": "perm-uuid",
    "name": "Criar API",
    "code": "api.create",
    "description": "Permite criar novas APIs",
    "resource": "api",
    "action": "create",
    "active": true,
    "createdAt": "2026-03-28T10:00:00",
    "updatedAt": "2026-03-28T10:00:00"
  }
]
```

#### Listar Permissões por Recurso (SUPER_ADMIN)

```http
GET /api/v1/permissions/resource/api
Authorization: Bearer {token}
```

#### Deletar Permissão (SUPER_ADMIN)

```http
DELETE /api/v1/permissions/{id}
Authorization: Bearer {token}
```

## Exemplos de Uso

### 1. Criar Sistema de Permissões Completo

```bash
# 1. Criar permissões para APIs
curl -X POST http://localhost:8080/api/v1/permissions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Criar API",
    "code": "api.create",
    "description": "Permite criar novas APIs",
    "resource": "api",
    "action": "create"
  }'

curl -X POST http://localhost:8080/api/v1/permissions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Editar API",
    "code": "api.update",
    "description": "Permite editar APIs",
    "resource": "api",
    "action": "update"
  }'

# 2. Criar role com permissões
curl -X POST http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "API Manager",
    "code": "API_MANAGER",
    "description": "Gerencia APIs do sistema",
    "permissionIds": ["perm-uuid-1", "perm-uuid-2"]
  }'

# 3. Atribuir role ao usuário
curl -X POST http://localhost:8080/api/v1/users/{userId}/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '["role-uuid"]'
```

### 2. Atualizar Perfil do Usuário

```bash
curl -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "João",
    "lastName": "Silva",
    "phoneNumber": "+238 999 9999",
    "bio": "Desenvolvedor Full Stack",
    "company": "Tech Company",
    "location": "Praia, Cabo Verde",
    "website": "https://joaosilva.dev"
  }'
```

### 3. Buscar Usuários por Role

```bash
curl -X GET "http://localhost:8080/api/v1/users/role/PROVIDER?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## Estrutura de Permissões Sugerida

### Resources
- `api` - APIs
- `user` - Usuários
- `role` - Roles
- `permission` - Permissões
- `category` - Categorias
- `subscription` - Subscrições
- `analytics` - Analytics

### Actions
- `create` - Criar
- `read` - Ler
- `update` - Atualizar
- `delete` - Deletar
- `publish` - Publicar
- `approve` - Aprovar
- `manage` - Gerenciar

### Exemplos de Códigos de Permissão
- `api.create` - Criar API
- `api.update` - Editar API
- `api.delete` - Deletar API
- `api.publish` - Publicar API
- `user.manage` - Gerenciar usuários
- `role.manage` - Gerenciar roles
- `analytics.read` - Ver analytics

## Roles do Sistema

### SUPER_ADMIN
- Acesso total ao sistema
- Não pode ser deletada
- Gerencia usuários, roles e permissões

### PROVIDER
- Cria e gerencia suas APIs
- Publica APIs
- Ver analytics das suas APIs

### CONSUMER
- Consome APIs públicas
- Solicita acesso a APIs privadas
- Ver documentação

## Cache

O módulo utiliza Redis para cache:

- `users`: 20 minutos
- `roles`: 1 hora
- `permissions`: 1 hora

O cache é invalidado automaticamente em operações de escrita.

## Auditoria

Todas as tabelas possuem auditoria automática:

- `createdAt`: Data de criação
- `updatedAt`: Data da última atualização
- `createdBy`: ID do usuário que criou
- `lastModifiedBy`: ID do usuário que modificou

## Boas Práticas

1. **Sempre sincronizar usuário no login**: Chame `/api/v1/users/me` após autenticação
2. **Usar códigos descritivos**: Use padrão `resource.action` para permissões
3. **Não deletar roles do sistema**: Roles com `isSystem=true` não podem ser deletadas
4. **Agrupar permissões em roles**: Não atribua permissões diretamente aos usuários
5. **Manter cache limpo**: O sistema invalida automaticamente, mas monitore o Redis

## Troubleshooting

### Usuário não aparece no banco

1. Verificar se o usuário fez login
2. Chamar endpoint `/api/v1/users/me`
3. Verificar logs da aplicação

### Permissões não funcionam

1. Verificar se a role tem as permissões corretas
2. Verificar se o usuário tem a role atribuída
3. Limpar cache do Redis

### Erro ao criar role

1. Verificar se o código já existe
2. Verificar se as permissões existem
3. Verificar se é SUPER_ADMIN

## Próximos Passos

- [ ] Implementar verificação de permissões nos endpoints
- [ ] Adicionar auditoria de mudanças de roles
- [ ] Implementar hierarquia de roles
- [ ] Adicionar permissões dinâmicas
- [ ] Implementar grupos de usuários
