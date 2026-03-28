# Roles e Grupos - Configuração Padrão

## Data
28 de Março de 2026

## Roles Disponíveis

O sistema possui 3 roles principais:

```java
public enum UserRole {
    SUPER_ADMIN,  // Administrador do sistema (acesso total)
    PROVIDER,     // Provedor de APIs (pode criar e gerenciar APIs)
    CONSUMER      // Consumidor de APIs (pode visualizar e consumir APIs) - PADRÃO
}
```

## Role Padrão: CONSUMER

### Registro de Novos Usuários

Quando um usuário se registra na plataforma, ele é **AUTOMATICAMENTE** atribuído ao grupo **CONSUMER**.

### Como Funciona

#### 1. Frontend - Não Envia Role

```typescript
// signup.component.ts
this.authService.register({
  email: 'user@example.com',
  password: 'password123',
  firstName: 'João',
  lastName: 'Silva'
  // ❌ NÃO envia role
}).subscribe(...)
```

#### 2. Backend - RegisterRequest

```java
@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UserRole role;  // Opcional, pode ser null
    
    /**
     * Retorna o role, padrão CONSUMER se não especificado
     */
    public UserRole getRoleOrDefault() {
        return role != null ? role : UserRole.CONSUMER;  // ✅ Padrão CONSUMER
    }
}
```

#### 3. Backend - KeycloakAdminService

```java
public void createUser(RegisterRequest request) {
    // Obter role (padrão CONSUMER)
    UserRole role = request.getRoleOrDefault();  // ✅ Sempre retorna CONSUMER se null
    log.info("Criando usuário {} com role: {}", request.getEmail(), role);
    
    // Criar usuário no Keycloak
    // ...
    
    // Atribuir role ao usuário
    assignRoleToUser(userId, role, adminToken);
    
    // Adicionar usuário ao grupo
    addUserToGroup(userId, role, adminToken);
}
```

### Logs Esperados

Ao registrar um novo usuário, você verá nos logs:

```
INFO: Criando usuário user@example.com com role: CONSUMER
INFO: Atribuindo role CONSUMER ao usuário <keycloak-user-id>
INFO: Adicionando usuário <keycloak-user-id> ao grupo CONSUMER
INFO: Utilizador criado com sucesso: user@example.com com role CONSUMER
```

## Grupos no Keycloak

### Estrutura de Grupos

O sistema cria automaticamente 3 grupos no Keycloak:

```
portal-api (realm)
├── SUPER_ADMIN (grupo)
├── PROVIDER (grupo)
└── CONSUMER (grupo)  ← Padrão para novos usuários
```

### Mapeamento Role → Grupo

```java
private String mapRoleToGroupName(UserRole role) {
    return switch (role) {
        case SUPER_ADMIN -> "SUPER_ADMIN";
        case PROVIDER -> "PROVIDER";
        case CONSUMER -> "CONSUMER";  // ✅ Padrão
    };
}
```

## Permissões por Role

### CONSUMER (Padrão)

**Pode:**
- Visualizar APIs públicas
- Visualizar categorias
- Visualizar documentação de APIs
- Consumir APIs (com API Key)
- Gerenciar seu próprio perfil

**Não pode:**
- Criar APIs
- Editar APIs
- Deletar APIs
- Gerenciar usuários
- Acessar painel administrativo

### PROVIDER

**Pode:**
- Tudo que CONSUMER pode
- Criar APIs
- Editar suas próprias APIs
- Deletar suas próprias APIs
- Gerenciar versões de APIs
- Gerenciar endpoints
- Visualizar estatísticas de suas APIs

**Não pode:**
- Editar APIs de outros providers
- Gerenciar usuários
- Acessar painel administrativo completo

### SUPER_ADMIN

**Pode:**
- Tudo que PROVIDER pode
- Gerenciar todos os usuários
- Atribuir roles
- Editar/deletar qualquer API
- Acessar painel administrativo completo
- Gerenciar categorias
- Visualizar auditoria completa
- Gerenciar permissões

## Alterando Role de um Usuário

### Manualmente no Keycloak

1. Acessar: http://localhost:8180/admin
2. Login: admin / admin
3. Realm: portal-api
4. Users > Selecionar usuário
5. Groups > Remover grupo atual
6. Groups > Adicionar novo grupo (PROVIDER ou SUPER_ADMIN)
7. Role Mappings > Atribuir nova role

### Via API (Apenas SUPER_ADMIN)

```bash
# Endpoint futuro (não implementado ainda)
PUT /api/v1/users/{userId}/role
Authorization: Bearer <super-admin-token>
Body:
{
  "role": "PROVIDER"
}
```

## Registro com Role Específica

### Apenas para Testes/Desenvolvimento

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@example.com",
    "password": "admin123",
    "role": "SUPER_ADMIN"  ← Especificar role
  }'
```

### Produção

Em produção, o endpoint de registro deve:
- **SEMPRE** ignorar o campo `role` enviado pelo frontend
- **SEMPRE** usar CONSUMER como padrão
- Apenas SUPER_ADMIN pode criar usuários com outras roles

**Recomendação de Segurança:**

```java
@PostMapping("/register")
public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
    // Forçar role CONSUMER em produção
    request.setRole(UserRole.CONSUMER);
    
    TokenResponse response = authService.register(request, ipAddress);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

## Verificando Role do Usuário

### No Token JWT

```json
{
  "sub": "keycloak-user-id",
  "email": "user@example.com",
  "realm_access": {
    "roles": [
      "CONSUMER",  ← Role do usuário
      "offline_access",
      "uma_authorization"
    ]
  },
  "groups": [
    "/CONSUMER"  ← Grupo do usuário
  ]
}
```

### No Banco de Dados

```sql
SELECT 
    u.email,
    u.first_name,
    u.last_name,
    r.name as role_name,
    r.code as role_code
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'user@example.com';
```

**Resultado esperado:**
```
email              | first_name | last_name | role_name | role_code
-------------------|------------|-----------|-----------|----------
user@example.com   | João       | Silva     | Consumer  | CONSUMER
```

## Fluxo Completo de Registro

```
1. Usuário preenche formulário
   ↓
2. Frontend envia: { email, password, firstName, lastName }
   ↓
3. Backend recebe RegisterRequest (role = null)
   ↓
4. getRoleOrDefault() retorna CONSUMER
   ↓
5. Cria usuário no Keycloak
   ↓
6. Atribui role CONSUMER
   ↓
7. Adiciona ao grupo CONSUMER
   ↓
8. Sincroniza com banco de dados
   ↓
9. Faz login automático
   ↓
10. Retorna tokens com user.roles = ["CONSUMER"]
```

## Testando

### 1. Registrar Novo Usuário

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "João",
    "lastName": "Silva",
    "email": "joao@example.com",
    "password": "password123"
  }'
```

### 2. Verificar Resposta

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "keycloak-user-id",
    "email": "joao@example.com",
    "roles": ["CONSUMER"]  ← ✅ Role CONSUMER atribuída
  }
}
```

### 3. Verificar no Keycloak

1. Acessar: http://localhost:8180/admin
2. Users > joao@example.com
3. Groups: Deve mostrar "CONSUMER"
4. Role Mappings: Deve mostrar "CONSUMER"

### 4. Verificar no Banco

```sql
SELECT * FROM users WHERE email = 'joao@example.com';
SELECT * FROM user_roles WHERE user_id = (SELECT id FROM users WHERE email = 'joao@example.com');
```

## Troubleshooting

### Usuário Criado Sem Role

**Sintoma:** Usuário não consegue acessar nenhum endpoint

**Causa:** Falha ao atribuir role no Keycloak

**Solução:**
1. Verificar logs: "Atribuindo role CONSUMER ao usuário"
2. Verificar se grupo CONSUMER existe no Keycloak
3. Recriar grupos se necessário

### Usuário com Role Errada

**Sintoma:** Usuário tem permissões incorretas

**Causa:** Role foi especificada manualmente no registro

**Solução:**
1. Remover campo `role` do frontend
2. Forçar `role = CONSUMER` no backend
3. Atualizar role manualmente no Keycloak

### Grupo Não Existe

**Sintoma:** Erro "Group not found"

**Causa:** Grupos não foram criados no Keycloak

**Solução:**
1. Executar `DataInitializerService` manualmente
2. Ou criar grupos manualmente no Keycloak:
   - SUPER_ADMIN
   - PROVIDER
   - CONSUMER

---

**Status**: ✅ Role CONSUMER atribuída automaticamente  
**Padrão**: Todos os novos usuários são CONSUMER  
**Segurança**: Apenas SUPER_ADMIN pode alterar roles
