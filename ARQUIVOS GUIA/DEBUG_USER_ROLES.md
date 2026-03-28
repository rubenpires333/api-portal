# Debug: User Roles não preenchido

## Problema
Após login, a tabela `user_roles` não está sendo preenchida.

## Verificações

### 1. Verificar se as Roles existem no banco

```sql
-- Verificar roles
SELECT id, code, name, is_system, active 
FROM roles 
ORDER BY code;
```

**Resultado esperado:**
```
id                                   | code        | name        | is_system | active
-------------------------------------|-------------|-------------|-----------|-------
abc-123...                           | CONSUMER    | Consumer    | true      | true
def-456...                           | PROVIDER    | Provider    | true      | true
ghi-789...                           | SUPER_ADMIN | Super Admin | true      | true
```

Se não houver roles, execute:
```sql
-- Verificar se DataInitializer executou
SELECT COUNT(*) FROM roles;
SELECT COUNT(*) FROM permissions;
```

### 2. Verificar se o usuário foi criado

```sql
-- Verificar usuário
SELECT id, keycloak_id, email, first_name, last_name, active 
FROM users 
WHERE email = 'seu-email@example.com';
```

### 3. Verificar user_roles

```sql
-- Verificar relacionamento
SELECT 
    u.id as user_id,
    u.email,
    ur.role_id,
    r.code as role_code,
    r.name as role_name
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'seu-email@example.com';
```

**Se role_id for NULL, o problema está confirmado.**

### 4. Verificar logs da aplicação

Procure por estas mensagens nos logs:

```
=== Iniciando createOrUpdateUser para: email@example.com ===
Usuário é novo? true
Criando novo usuário...
✓ Role CONSUMER encontrada: ID=abc-123, Nome=Consumer
Usuário criado em memória: email@example.com
✓ Role CONSUMER adicionada ao Set de roles do usuário
Total de roles no usuário antes de salvar: 1
✓ Usuário salvo no banco com ID: xyz-789
Total de roles no usuário após salvar: 1
✓✓✓ SUCESSO: Role CONSUMER atribuída ao novo usuário: email@example.com
=== Finalizando createOrUpdateUser ===
```

**Se aparecer:**
```
❌ ERRO: Role CONSUMER não encontrada no banco de dados!
Total de roles no banco: 0
```

Significa que o DataInitializer não executou. Reinicie a aplicação.

## Soluções

### Solução 1: Reiniciar aplicação

O `DataInitializerService` executa automaticamente ao iniciar. Se as roles não existem:

1. Pare a aplicação
2. Inicie novamente
3. Verifique os logs:
   ```
   === Iniciando verificação de dados do sistema ===
   Verificando permissões do sistema...
   ✓ 26 novas permissões criadas
   Verificando roles do sistema...
   ✓ Role criada: SUPER_ADMIN com 26 permissões
   ✓ Role criada: PROVIDER com 7 permissões
   ✓ Role criada: CONSUMER com 3 permissões
   === Verificação de dados concluída ===
   ```

### Solução 2: Criar roles manualmente

Se o DataInitializer não executar, crie as roles manualmente:

```sql
-- Inserir role CONSUMER
INSERT INTO roles (id, code, name, description, is_system, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'CONSUMER',
    'Consumer',
    'Consumidor de APIs',
    true,
    true,
    NOW(),
    NOW()
);

-- Inserir role PROVIDER
INSERT INTO roles (id, code, name, description, is_system, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'PROVIDER',
    'Provider',
    'Provedor de APIs',
    true,
    true,
    NOW(),
    NOW()
);

-- Inserir role SUPER_ADMIN
INSERT INTO roles (id, code, name, description, is_system, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'SUPER_ADMIN',
    'Super Admin',
    'Administrador com acesso total ao sistema',
    true,
    true,
    NOW(),
    NOW()
);
```

### Solução 3: Atribuir role manualmente ao usuário existente

Se o usuário já foi criado sem role:

```sql
-- 1. Obter IDs
SELECT id FROM users WHERE email = 'seu-email@example.com';  -- user_id
SELECT id FROM roles WHERE code = 'CONSUMER';                 -- role_id

-- 2. Inserir relacionamento
INSERT INTO user_roles (user_id, role_id)
VALUES (
    'user_id_aqui',
    'role_id_aqui'
);
```

### Solução 4: Deletar usuário e fazer login novamente

```sql
-- Deletar relacionamentos primeiro
DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE email = 'seu-email@example.com');

-- Deletar usuário
DELETE FROM users WHERE email = 'seu-email@example.com';
```

Depois faça login novamente. O usuário será recriado com a role.

## Teste Completo

### Passo 1: Limpar dados
```sql
DELETE FROM user_roles;
DELETE FROM users;
```

### Passo 2: Verificar roles
```sql
SELECT COUNT(*) FROM roles;  -- Deve ser >= 3
```

Se for 0, reinicie a aplicação.

### Passo 3: Fazer login
```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "teste@example.com",
  "password": "senha123"
}
```

### Passo 4: Verificar logs

Procure por:
```
=== Iniciando createOrUpdateUser para: teste@example.com ===
...
✓✓✓ SUCESSO: Role CONSUMER atribuída ao novo usuário: teste@example.com
```

### Passo 5: Verificar banco
```sql
SELECT 
    u.email,
    r.code as role_code
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'teste@example.com';
```

**Resultado esperado:**
```
email              | role_code
-------------------|----------
teste@example.com  | CONSUMER
```

## Possíveis Causas

### 1. DataInitializer não executou
- Verificar se a aplicação iniciou completamente
- Verificar logs de inicialização
- Verificar se há erros de conexão com banco

### 2. Transação não foi commitada
- Verificar se há `@Transactional` no método
- Verificar se não há exceções sendo lançadas
- Verificar logs de erro

### 3. Cascade não configurado
- Verificar se `cascade = {CascadeType.PERSIST, CascadeType.MERGE}` está no relacionamento
- Verificar se `fetch = FetchType.EAGER` está configurado

### 4. Cache interferindo
- Limpar cache Redis: `redis-cli FLUSHALL`
- Desabilitar cache temporariamente

### 5. Usuário já existia antes da correção
- Deletar usuário e fazer login novamente
- Ou atribuir role manualmente via SQL

## Endpoint de Debug (Criar temporariamente)

Adicione este endpoint no `UserController` para debug:

```java
@GetMapping("/debug/roles")
@Operation(summary = "Debug - Verificar roles no sistema")
public ResponseEntity<Map<String, Object>> debugRoles() {
    Map<String, Object> debug = new HashMap<>();
    
    // Contar roles
    long roleCount = roleRepository.count();
    debug.put("totalRoles", roleCount);
    
    // Listar roles
    List<Role> roles = roleRepository.findAll();
    debug.put("roles", roles.stream()
        .map(r -> Map.of(
            "id", r.getId(),
            "code", r.getCode(),
            "name", r.getName()
        ))
        .collect(Collectors.toList()));
    
    // Contar usuários
    long userCount = userRepository.count();
    debug.put("totalUsers", userCount);
    
    // Contar user_roles
    long userRoleCount = userRepository.findAll().stream()
        .mapToLong(u -> u.getRoles().size())
        .sum();
    debug.put("totalUserRoles", userRoleCount);
    
    return ResponseEntity.ok(debug);
}
```

Chame: `GET http://localhost:8080/api/v1/users/debug/roles`

## Contato

Se o problema persistir após todas as verificações, forneça:
1. Logs completos da aplicação
2. Resultado das queries SQL
3. Versão do PostgreSQL
4. Versão do Spring Boot
