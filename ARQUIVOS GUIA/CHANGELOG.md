# Changelog - API Portal Backend

## [2026-03-28] - Correções de Auditoria e Atribuição de Roles

### Corrigido

#### Sistema de Auditoria
- **userId e userEmail agora são preenchidos corretamente** no AuditLog
- Dados do JWT (userId e userEmail) são extraídos no `AuditInterceptor` ANTES de chamar método assíncrono
- Novos métodos `extractUserId()` e `extractUserEmail()` no `AuditInterceptor`
- Método `logRequest()` do `AuditService` agora recebe userId e userEmail como parâmetros
- Corrigido problema de "request reciclado" que causava valores NULL

#### Atribuição de Roles
- **Role CONSUMER é automaticamente atribuída** a novos usuários após sincronização
- Tabela `user_roles` agora é preenchida automaticamente
- Usuários criados via login ou OAuth2 recebem role padrão
- Log detalhado da atribuição de role

### Modificado

#### UserService
- Método `createOrUpdateUser()` agora verifica se é novo usuário
- Se for novo usuário, atribui automaticamente a role CONSUMER
- Role é buscada do banco de dados via `RoleRepository.findByCode("CONSUMER")`
- Log de confirmação quando role é atribuída

#### AuditInterceptor
- Adicionados imports: `Authentication`, `SecurityContextHolder`, `Jwt`
- Método `afterCompletion()` agora extrai userId e userEmail antes de chamar `logRequest()`
- Novos métodos privados:
  - `extractUserId()`: Extrai subject do JWT
  - `extractUserEmail()`: Extrai email do JWT
- Tratamento de exceções para casos onde JWT não está disponível

#### AuditService
- Removidos imports: `Authentication`, `SecurityContextHolder`, `Jwt`
- Método `logRequest()` agora recebe `userId` e `userEmail` como primeiros parâmetros
- Não tenta mais extrair dados do JWT dentro do método assíncrono

### Fluxo Corrigido

#### Criação de Usuário com Role
1. Usuário faz login/OAuth2
2. Backend sincroniza usuário via `UserService.createOrUpdateUser()`
3. Se for novo usuário:
   - Cria registro na tabela `users`
   - Busca role CONSUMER no banco
   - Adiciona role ao Set de roles do usuário
   - Salva usuário (JPA cria registro em `user_roles` automaticamente)
4. Log: "✓ Role CONSUMER atribuída ao novo usuário: email@example.com"

#### Auditoria com userId e userEmail
1. Request chega ao backend
2. `AuditInterceptor.preHandle()` registra tempo de início
3. Request é processado
4. `AuditInterceptor.afterCompletion()` é chamado
5. Interceptor extrai dados do JWT:
   - `userId` via `jwt.getSubject()`
   - `userEmail` via `jwt.getClaimAsString("email")`
6. Interceptor chama `auditService.logRequest()` com userId e userEmail
7. AuditService salva log com userId e userEmail preenchidos

### Logs de Exemplo

#### Novo Usuário com Role
```
INFO  - Criando/atualizando usuário: user@example.com
INFO  - ✓ Role CONSUMER atribuída ao novo usuário: user@example.com
```

#### Usuário Existente
```
INFO  - Criando/atualizando usuário: user@example.com
```

#### Auditoria com Usuário
```
DEBUG - userId extraído: abc-123-def
DEBUG - userEmail extraído: user@example.com
```

### Estrutura de Dados

#### Tabela user_roles (preenchida automaticamente)
```sql
user_id (UUID, FK -> users.id)
role_id (UUID, FK -> roles.id)
```

#### Tabela audit_logs (com userId e userEmail)
```sql
id (UUID, PK)
timestamp (DateTime)
user_id (String) -- Agora preenchido!
user_email (String) -- Agora preenchido!
method (String)
endpoint (String)
...
```

### Benefícios

✅ Auditoria completa com identificação do usuário  
✅ Rastreamento de ações por usuário  
✅ Roles atribuídas automaticamente  
✅ Tabela user_roles preenchida corretamente  
✅ Sem necessidade de atribuição manual de roles  
✅ Logs detalhados para debugging

---

## [2026-03-28] - Sincronização Automática de Usuários

### Adicionado

#### Sincronização Automática após Login
- Usuários agora são automaticamente sincronizados e salvos na base de dados após login
- Sincronização também ocorre após autenticação OAuth2
- IP do cliente é registrado automaticamente no último login

#### Modificações no AuthService
- Método `login()` agora aceita parâmetro `ipAddress`
- Método `register()` agora aceita parâmetro `ipAddress`
- Adicionado método privado `syncUserFromJwt()` para sincronização automática
- Token JWT é decodificado após login para extrair informações do usuário
- Usuário é criado/atualizado automaticamente na base de dados

#### Modificações no AuthController
- Endpoint `/api/v1/auth/login` agora captura IP do cliente
- Endpoint `/api/v1/auth/register` agora captura IP do cliente
- Adicionado `HttpServletRequest` como parâmetro nos métodos

#### Modificações no OAuth2Controller
- Adicionado endpoint `POST /api/v1/auth/oauth2/callback` para processar callback OAuth2
- Callback troca código de autorização por token JWT
- Usuário é sincronizado automaticamente após autenticação OAuth2
- IP do cliente é registrado no último login

#### Dependências Adicionadas
- `JwtDecoder` injetado no `AuthService` e `OAuth2Controller`
- `UserService` injetado no `AuthService` e `OAuth2Controller`

### Fluxo de Sincronização

#### Login Normal
1. Usuário envia credenciais para `/api/v1/auth/login`
2. Backend autentica com Keycloak e recebe token JWT
3. Backend decodifica o token JWT
4. Backend extrai informações do usuário (keycloakId, email, nome, etc)
5. Backend cria/atualiza usuário na base de dados
6. Backend registra IP e data/hora do último login
7. Backend retorna token para o frontend

#### OAuth2
1. Frontend redireciona usuário para provedor OAuth2 (Google, LinkedIn, etc)
2. Usuário autentica no provedor
3. Provedor redireciona para frontend com código de autorização
4. Frontend envia código para `/api/v1/auth/oauth2/callback`
5. Backend troca código por token JWT no Keycloak
6. Backend decodifica o token JWT
7. Backend extrai informações do usuário
8. Backend cria/atualiza usuário na base de dados
9. Backend registra IP e data/hora do último login
10. Backend retorna token para o frontend

#### Registro
1. Usuário envia dados para `/api/v1/auth/register`
2. Backend cria usuário no Keycloak
3. Backend faz login automático
4. Backend sincroniza usuário (mesmo fluxo do login)
5. Backend retorna token para o frontend

### Benefícios

- Usuários são automaticamente criados na base de dados no primeiro login
- Não é mais necessário chamar `/api/v1/users/me` manualmente
- Rastreamento automático de último login (data/hora e IP)
- Funciona tanto para login normal quanto OAuth2
- Sincronização transparente para o frontend

### Correção de Bugs

- Corrigido erro "The request object has been recycled" no sistema de auditoria
- Dados do request agora são extraídos antes de chamar método assíncrono

---

## [2026-03-28] - Data Initializer Service

### Adicionado

#### DataInitializerService
- Serviço que executa automaticamente ao iniciar a aplicação
- Verifica e cria permissões padrão se não existirem
- Verifica e cria roles do sistema se não existirem
- Suporte para permissões customizadas via `application.properties`
- Logs detalhados de todas as operações
- Operação idempotente (pode executar múltiplas vezes sem duplicar)

#### PermissionConfig
- Configuração para adicionar permissões customizadas
- Suporte via `application.properties`
- Formato: `app.permissions.custom[index].propriedade=valor`

#### Permissões Padrão (26 total)
- APIs: create, read, update, delete, publish
- Categorias: create, read, update, delete
- Usuários: manage, read, update
- Roles: manage, read
- Permissões: manage, read
- Auditoria: read
- Versões: create, read, update, delete
- Endpoints: create, read, update, delete

#### Roles Padrão
- `SUPER_ADMIN`: Todas as 26 permissões
- `PROVIDER`: 7 permissões (APIs + leitura)
- `CONSUMER`: 3 permissões (apenas leitura)

#### Documentação
- `DATA_INITIALIZER_GUIA.md`: Guia completo
- Exemplos de uso no `application.properties`
- Instruções para adicionar permissões customizadas

### Como Usar

#### Adicionar Permissões Customizadas

No `application.properties`:

```properties
app.permissions.custom[0].name=Exportar Relatório
app.permissions.custom[0].code=report.export
app.permissions.custom[0].description=Permite exportar relatórios
app.permissions.custom[0].resource=report
app.permissions.custom[0].action=export
```

#### Logs de Inicialização

```
=== Iniciando verificação de dados do sistema ===
Verificando permissões do sistema...
✓ 26 novas permissões criadas
Total de permissões no sistema: 26

Verificando roles do sistema...
✓ Role criada: SUPER_ADMIN com 26 permissões
✓ Role criada: PROVIDER com 7 permissões
✓ Role criada: CONSUMER com 3 permissões
Total de roles no sistema: 3

=== Verificação de dados concluída ===
```

---

## [2026-03-28] - Módulo User e Sistema de Permissões

### Adicionado

#### Módulo User
- **Entidades**:
  - `User`: Usuários sincronizados do Keycloak
  - `Role`: Papéis/funções do sistema
  - `Permission`: Permissões granulares
  
- **Repositories**:
  - `UserRepository`: Queries customizadas para busca e filtros
  - `RoleRepository`: Gerenciamento de roles
  - `PermissionRepository`: Gerenciamento de permissões
  
- **Services**:
  - `UserService`: CRUD de usuários, atribuição de roles, cache
  - `RoleService`: CRUD de roles, associação com permissões
  - `PermissionService`: CRUD de permissões
  - `KeycloakSyncService`: Sincronização automática com Keycloak
  
- **Controllers**:
  - `UserController`: 11 endpoints para gerenciamento de usuários
  - `RoleController`: 5 endpoints para gerenciamento de roles
  - `PermissionController`: 4 endpoints para gerenciamento de permissões
  
- **DTOs**:
  - `UserResponse`, `UpdateUserRequest`
  - `RoleResponse`, `RoleRequest`
  - `PermissionResponse`, `PermissionRequest`

#### Funcionalidades
- Sincronização automática de usuários do Keycloak no primeiro login
- Sistema de roles e permissões granular (resource.action)
- Rastreamento de último login (data/hora e IP)
- Perfil de usuário customizável (bio, company, location, website)
- Cache Redis para users, roles e permissions
- Auditoria completa em todas as tabelas
- Endpoint `/api/v1/users/me` para dados do usuário autenticado

#### Roles do Sistema
- `SUPER_ADMIN`: Acesso total (não pode ser deletada)
- `PROVIDER`: Gerencia suas APIs
- `CONSUMER`: Consome APIs públicas

#### Permissões Padrão
- APIs: create, read, update, delete, publish
- Categorias: create, read, update, delete
- Usuários: manage, read, update
- Roles: manage, read
- Permissões: manage, read
- Auditoria: read

#### Documentação
- `MODULO_USER_GUIA.md`: Guia completo com exemplos
- `V2__init_roles_permissions.sql`: Script de inicialização
- Atualizado `README.md` com informações do módulo

#### Cache Redis
- `users`: 20 minutos
- `roles`: 1 hora
- `permissions`: 1 hora

### Modificado

#### Auditoria
- Todas as entidades do módulo User estendem `Auditable`
- Campos automáticos: createdAt, updatedAt, createdBy, lastModifiedBy

#### RedisConfig
- Adicionado cache para users, roles e permissions
- TTLs configurados por tipo de cache

### Estrutura de Tabelas

```
users
├── id (UUID, PK)
├── keycloak_id (String, Unique)
├── email (String, Unique)
├── first_name (String)
├── last_name (String)
├── username (String)
├── phone_number (String)
├── avatar_url (String)
├── email_verified (Boolean)
├── active (Boolean)
├── last_login_at (DateTime)
├── last_login_ip (String)
├── bio (String)
├── company (String)
├── location (String)
├── website (String)
├── created_at (DateTime)
├── updated_at (DateTime)
├── created_by (String)
└── last_modified_by (String)

roles
├── id (UUID, PK)
├── name (String, Unique)
├── code (String, Unique)
├── description (String)
├── is_system (Boolean)
├── active (Boolean)
├── created_at (DateTime)
├── updated_at (DateTime)
├── created_by (String)
└── last_modified_by (String)

permissions
├── id (UUID, PK)
├── name (String, Unique)
├── code (String, Unique)
├── description (String)
├── resource (String)
├── action (String)
├── active (Boolean)
├── created_at (DateTime)
├── updated_at (DateTime)
├── created_by (String)
└── last_modified_by (String)

user_roles (N:N)
├── user_id (UUID, FK)
└── role_id (UUID, FK)

role_permissions (N:N)
├── role_id (UUID, FK)
└── permission_id (UUID, FK)
```

### Fluxo de Sincronização

1. Usuário faz login no Keycloak
2. Frontend recebe JWT token
3. Frontend chama `/api/v1/users/me`
4. Backend extrai dados do JWT
5. Backend cria/atualiza usuário no banco
6. Backend registra último login
7. Backend retorna dados do usuário

### Endpoints Principais

#### Usuários
- `GET /api/v1/users/me` - Dados do usuário autenticado
- `PUT /api/v1/users/me` - Atualizar perfil
- `GET /api/v1/users` - Listar usuários (SUPER_ADMIN)
- `GET /api/v1/users/search` - Buscar usuários (SUPER_ADMIN)
- `POST /api/v1/users/{id}/roles` - Atribuir roles (SUPER_ADMIN)
- `POST /api/v1/users/{id}/deactivate` - Desativar (SUPER_ADMIN)
- `POST /api/v1/users/{id}/activate` - Ativar (SUPER_ADMIN)

#### Roles
- `POST /api/v1/roles` - Criar role (SUPER_ADMIN)
- `GET /api/v1/roles` - Listar roles (SUPER_ADMIN)
- `GET /api/v1/roles/{id}` - Obter role (SUPER_ADMIN)
- `PUT /api/v1/roles/{id}` - Atualizar role (SUPER_ADMIN)
- `DELETE /api/v1/roles/{id}` - Deletar role (SUPER_ADMIN)

#### Permissões
- `POST /api/v1/permissions` - Criar permissão (SUPER_ADMIN)
- `GET /api/v1/permissions` - Listar permissões (SUPER_ADMIN)
- `GET /api/v1/permissions/resource/{resource}` - Por recurso (SUPER_ADMIN)
- `DELETE /api/v1/permissions/{id}` - Deletar permissão (SUPER_ADMIN)

### Próximos Passos

- [ ] Implementar verificação de permissões nos endpoints existentes
- [ ] Adicionar auditoria de mudanças de roles
- [ ] Implementar hierarquia de roles
- [ ] Adicionar permissões dinâmicas
- [ ] Implementar grupos de usuários
- [ ] Dashboard de gerenciamento de usuários

---

## [2026-03-28] - Sistema de Auditoria

### Adicionado

- Classe base `Auditable` para auditoria automática
- Entidade `AuditLog` para logs de requisições
- `AuditService` com processamento assíncrono
- `AuditInterceptor` para capturar requisições
- `ContentCachingFilter` para capturar request/response body
- `AuditController` com endpoints de consulta
- Cache Redis para otimização
- Documentação completa em `SISTEMA_AUDITORIA.md`

### Modificado

- Todas as entidades principais agora estendem `Auditable`
- `docker-compose.yml` com serviço Redis e volume persistente
- `RedisConfig` com TTLs customizados por cache
- `application.properties` com configurações Redis

---

## [2026-03-28] - Módulo API

### Adicionado

- CRUD completo de APIs, Versões, Endpoints e Categorias
- 26 endpoints REST
- Sistema de versionamento
- Busca e filtros avançados
- Controle de acesso por role
- Documentação em `MODULO_API_GUIA_USO.md`

---

## [2026-03-28] - Módulo Auth

### Adicionado

- Login/Registro com Keycloak
- OAuth2 (Google, LinkedIn, GitHub)
- API Keys
- Atualização de perfil e senha
- Grupos e Roles
- Documentação em `OAUTH2_SETUP.md`
