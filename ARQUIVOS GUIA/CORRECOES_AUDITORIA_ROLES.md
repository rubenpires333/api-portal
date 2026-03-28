# Correções: Auditoria e Atribuição de Roles

## Problemas Identificados

### 1. userId e userEmail NULL no AuditLog
**Problema:** Os campos `userId` e `userEmail` na tabela `audit_logs` ficavam NULL mesmo com usuário autenticado.

**Causa:** O método `logRequest()` do `AuditService` é assíncrono (`@Async`). Quando executado em thread separada, o `SecurityContextHolder` não tinha acesso ao contexto de segurança da thread original.

**Solução:** Extrair `userId` e `userEmail` do JWT no `AuditInterceptor` (thread principal) ANTES de chamar o método assíncrono, e passar como parâmetros.

### 2. Tabela user_roles vazia
**Problema:** Após login/OAuth2, o usuário era criado na tabela `users`, mas nenhuma role era atribuída. A tabela `user_roles` ficava vazia.

**Causa:** O método `createOrUpdateUser()` criava o usuário com `roles = new HashSet<>()` vazio, mas não atribuía nenhuma role padrão.

**Solução:** Ao criar novo usuário, buscar a role CONSUMER no banco e adicioná-la ao Set de roles antes de salvar.

## Implementação das Correções

### 1. Correção do AuditLog

#### AuditInterceptor.java

**Antes:**
```java
// Chamava método assíncrono sem passar userId e userEmail
auditService.logRequest(
    method,
    uri,
    queryParams,
    ipAddress,
    userAgent,
    response.getStatus(),
    executionTime,
    requestBody,
    responseBody,
    errorMessage,
    stackTrace
);
```

**Depois:**
```java
// Extrai userId e userEmail ANTES de chamar método assíncrono
String userId = extractUserId(request);
String userEmail = extractUserEmail(request);

auditService.logRequest(
    userId,        // Novo parâmetro
    userEmail,     // Novo parâmetro
    method,
    uri,
    queryParams,
    ipAddress,
    userAgent,
    response.getStatus(),
    executionTime,
    requestBody,
    responseBody,
    errorMessage,
    stackTrace
);
```

**Novos métodos:**
```java
private String extractUserId(HttpServletRequest request) {
    try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
    } catch (Exception e) {
        log.debug("Não foi possível extrair userId: {}", e.getMessage());
    }
    return null;
}

private String extractUserEmail(HttpServletRequest request) {
    try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
    } catch (Exception e) {
        log.debug("Não foi possível extrair userEmail: {}", e.getMessage());
    }
    return null;
}
```

#### AuditService.java

**Antes:**
```java
@Async
@Transactional
public void logRequest(
        String method,
        String uri,
        // ... outros parâmetros
        ) {
    
    // Tentava extrair do SecurityContext (não funciona em thread assíncrona)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = null;
    String userEmail = null;
    
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        userId = jwt.getSubject();
        userEmail = jwt.getClaimAsString("email");
    }
    
    // Criava AuditLog com userId e userEmail (sempre NULL)
}
```

**Depois:**
```java
@Async
@Transactional
public void logRequest(
        String userId,      // Recebe como parâmetro
        String userEmail,   // Recebe como parâmetro
        String method,
        String uri,
        // ... outros parâmetros
        ) {
    
    // Usa diretamente os valores recebidos
    AuditLog auditLog = AuditLog.builder()
        .userId(userId)
        .userEmail(userEmail)
        // ... outros campos
        .build();
}
```

### 2. Correção da Atribuição de Roles

#### UserService.java

**Antes:**
```java
@Transactional
@CacheEvict(value = "users", allEntries = true)
public User createOrUpdateUser(String keycloakId, String email, String firstName, 
                               String lastName, String username, Boolean emailVerified) {
    
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElse(User.builder()
            .keycloakId(keycloakId)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .username(username)
            .emailVerified(emailVerified != null ? emailVerified : false)
            .active(true)
            .roles(new HashSet<>())  // Vazio! Nenhuma role atribuída
            .build());
    
    // Atualizar informações se já existir
    if (user.getId() != null) {
        user.setEmail(email);
        // ...
    }
    
    return userRepository.save(user);
}
```

**Depois:**
```java
@Transactional
@CacheEvict(value = "users", allEntries = true)
public User createOrUpdateUser(String keycloakId, String email, String firstName, 
                               String lastName, String username, Boolean emailVerified) {
    
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElse(null);
    
    boolean isNewUser = (user == null);
    
    if (isNewUser) {
        // Criar novo usuário
        user = User.builder()
            .keycloakId(keycloakId)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .username(username)
            .emailVerified(emailVerified != null ? emailVerified : false)
            .active(true)
            .roles(new HashSet<>())
            .build();
        
        // Atribuir role padrão CONSUMER
        Role consumerRole = roleRepository.findByCode("CONSUMER")
            .orElse(null);
        
        if (consumerRole != null) {
            user.getRoles().add(consumerRole);
            log.info("✓ Role CONSUMER atribuída ao novo usuário: {}", email);
        } else {
            log.warn("⚠ Role CONSUMER não encontrada. Usuário criado sem role.");
        }
    } else {
        // Atualizar informações se já existir
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        if (emailVerified != null) {
            user.setEmailVerified(emailVerified);
        }
    }
    
    return userRepository.save(user);
}
```

## Como Funciona Agora

### Fluxo de Auditoria

```
1. Request HTTP chega
   ↓
2. AuditInterceptor.preHandle()
   - Registra tempo de início
   ↓
3. Request é processado (Controller → Service → Repository)
   ↓
4. AuditInterceptor.afterCompletion()
   - Extrai dados do request (method, uri, body, etc)
   - Extrai userId do JWT via SecurityContextHolder
   - Extrai userEmail do JWT via SecurityContextHolder
   ↓
5. Chama AuditService.logRequest() com todos os dados
   ↓
6. AuditService executa em thread assíncrona
   - Cria AuditLog com userId e userEmail já extraídos
   - Salva no banco de dados
   ↓
7. AuditLog salvo com userId e userEmail preenchidos ✓
```

### Fluxo de Atribuição de Role

```
1. Usuário faz login/OAuth2
   ↓
2. Backend autentica e recebe JWT
   ↓
3. Backend chama UserService.createOrUpdateUser()
   ↓
4. UserService verifica se usuário existe
   ↓
5. Se NÃO existe (novo usuário):
   - Cria objeto User
   - Busca role CONSUMER no banco
   - Adiciona role ao Set de roles
   - Salva usuário
   - JPA cria registro em user_roles automaticamente
   ↓
6. Se JÁ existe:
   - Atualiza informações
   - Mantém roles existentes
   ↓
7. Usuário salvo com role CONSUMER ✓
```

## Verificação

### 1. Verificar AuditLog com userId

```sql
-- Verificar logs de auditoria
SELECT 
    id,
    timestamp,
    user_id,        -- Deve estar preenchido
    user_email,     -- Deve estar preenchido
    method,
    endpoint,
    status_code
FROM audit_logs
ORDER BY timestamp DESC
LIMIT 10;
```

**Resultado esperado:**
```
id                                   | timestamp           | user_id      | user_email          | method | endpoint           | status_code
-------------------------------------|---------------------|--------------|---------------------|--------|--------------------|------------
abc-123-def...                       | 2026-03-28 12:30:00 | xyz-789-abc  | user@example.com    | GET    | /api/v1/users/me   | 200
```

### 2. Verificar user_roles

```sql
-- Verificar roles atribuídas
SELECT 
    u.id,
    u.email,
    u.first_name,
    r.code as role_code,
    r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
ORDER BY u.email;
```

**Resultado esperado:**
```
id                  | email              | first_name | role_code | role_name
--------------------|--------------------|-----------|-----------|-----------
abc-123-def...      | user@example.com   | João      | CONSUMER  | Consumer
```

### 3. Verificar Logs da Aplicação

**Novo usuário criado:**
```
INFO  - Criando/atualizando usuário: user@example.com
INFO  - ✓ Role CONSUMER atribuída ao novo usuário: user@example.com
INFO  - Atualizando último login do usuário: abc-123-def
```

**Usuário existente:**
```
INFO  - Criando/atualizando usuário: user@example.com
INFO  - Atualizando último login do usuário: abc-123-def
```

## Testes

### Teste 1: Criar Novo Usuário

1. Fazer logout (se estiver logado)
2. Registrar novo usuário via `/api/v1/auth/register`
3. Verificar logs da aplicação
4. Verificar banco de dados:
   ```sql
   SELECT * FROM users WHERE email = 'novousuario@example.com';
   SELECT * FROM user_roles WHERE user_id = '<id_do_usuario>';
   ```
5. Resultado esperado: Usuário criado com role CONSUMER

### Teste 2: Login com Usuário Existente

1. Fazer login via `/api/v1/auth/login`
2. Verificar logs da aplicação
3. Verificar banco de dados:
   ```sql
   SELECT * FROM users WHERE email = 'usuario@example.com';
   SELECT * FROM user_roles WHERE user_id = '<id_do_usuario>';
   ```
4. Resultado esperado: Roles mantidas, last_login_at atualizado

### Teste 3: Auditoria com Usuário

1. Fazer login via `/api/v1/auth/login`
2. Fazer algumas requisições autenticadas
3. Verificar banco de dados:
   ```sql
   SELECT user_id, user_email, endpoint, status_code 
   FROM audit_logs 
   WHERE user_email = 'usuario@example.com'
   ORDER BY timestamp DESC;
   ```
4. Resultado esperado: Logs com user_id e user_email preenchidos

### Teste 4: OAuth2

1. Fazer login via Google/LinkedIn
2. Verificar logs da aplicação
3. Verificar banco de dados
4. Resultado esperado: Usuário criado com role CONSUMER

## Troubleshooting

### userId e userEmail ainda NULL

**Possíveis causas:**
1. JWT não está sendo enviado no header Authorization
2. JWT está inválido ou expirado
3. SecurityContextHolder não está configurado

**Solução:**
1. Verificar se o token está sendo enviado: `Authorization: Bearer <token>`
2. Verificar logs: `DEBUG - Não foi possível extrair userId: ...`
3. Verificar configuração do Spring Security

### Role não está sendo atribuída

**Possíveis causas:**
1. Role CONSUMER não existe no banco
2. DataInitializerService não executou
3. Erro ao salvar usuário

**Solução:**
1. Verificar se role existe: `SELECT * FROM roles WHERE code = 'CONSUMER';`
2. Executar DataInitializerService manualmente
3. Verificar logs: `⚠ Role CONSUMER não encontrada`

### Tabela user_roles vazia

**Possíveis causas:**
1. Usuário foi criado antes da correção
2. Role não foi adicionada ao Set de roles
3. Erro ao salvar

**Solução:**
1. Fazer logout e login novamente
2. Verificar logs da aplicação
3. Atribuir role manualmente via endpoint `/api/v1/users/{id}/roles`

## Próximos Passos

- [ ] Adicionar endpoint para alterar role do usuário
- [ ] Implementar role PROVIDER para criadores de APIs
- [ ] Adicionar role SUPER_ADMIN para administradores
- [ ] Implementar verificação de permissões nos endpoints
- [ ] Adicionar auditoria de mudanças de roles
- [ ] Dashboard de auditoria com filtros por usuário
