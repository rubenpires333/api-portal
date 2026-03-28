# Exemplo Prático: Migração para Sistema de Permissões

## Antes vs Depois

### ApiController - ANTES (usando @PreAuthorize com roles)

```java
@RestController
@RequestMapping("/api/v1/apis")
@RequiredArgsConstructor
public class ApiController {
    
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN')")
    public ResponseEntity<List<ApiResponse>> getMyApis(Authentication auth) {
        return ResponseEntity.ok(apiService.getMyApis(getUserId(auth)));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> createApi(@RequestBody ApiRequest request) {
        return ResponseEntity.ok(apiService.create(request));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> updateApi(@PathVariable UUID id, @RequestBody ApiRequest request) {
        return ResponseEntity.ok(apiService.update(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteApi(@PathVariable UUID id) {
        apiService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### ApiController - DEPOIS (usando @RequiresPermission)

```java
@RestController
@RequestMapping("/api/v1/apis")
@RequiredArgsConstructor
public class ApiController {
    
    @GetMapping("/my")
    @RequiresPermission("api.read")
    public ResponseEntity<List<ApiResponse>> getMyApis(Authentication auth) {
        return ResponseEntity.ok(apiService.getMyApis(getUserId(auth)));
    }
    
    @PostMapping
    @RequiresPermission("api.create")
    public ResponseEntity<ApiResponse> createApi(@RequestBody ApiRequest request) {
        return ResponseEntity.ok(apiService.create(request));
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("api.update")
    public ResponseEntity<ApiResponse> updateApi(@PathVariable UUID id, @RequestBody ApiRequest request) {
        return ResponseEntity.ok(apiService.update(id, request));
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("api.delete")
    public ResponseEntity<Void> deleteApi(@PathVariable UUID id) {
        apiService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Vantagens da Mudança

### 1. Flexibilidade
**Antes**: Para dar permissão de deletar API para PROVIDER, precisava mudar código:
```java
@PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN')") // Recompilar!
```

**Depois**: Apenas adicione a permissão no banco:
```sql
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'PROVIDER' AND p.code = 'api.delete';
```

### 2. Granularidade
**Antes**: Ou tem acesso a TUDO da role ou a NADA
```java
@PreAuthorize("hasRole('PROVIDER')") // Acesso a tudo de PROVIDER
```

**Depois**: Controle fino por operação
```java
@RequiresPermission("api.create")  // Apenas criar
@RequiresPermission("api.publish") // Apenas publicar
```

### 3. Auditoria
**Antes**: Difícil saber quais roles têm acesso a quê
```java
// Precisa ler código para saber
@PreAuthorize("hasAnyRole('PROVIDER', 'SUPER_ADMIN', 'MODERATOR')")
```

**Depois**: Query simples no banco
```sql
SELECT r.name, p.code, p.name
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE p.resource = 'api'
ORDER BY r.name, p.action;
```

## UserController - Exemplo Completo

### ANTES
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }
    
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable UUID id, @RequestBody Set<UUID> roleIds) {
        return ResponseEntity.ok(userService.assignRoles(id, roleIds));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### DEPOIS (Opção 1: Permissão por método)
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping
    @RequiresPermission("user.read")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(pageable));
    }
    
    @GetMapping("/{id}")
    @RequiresPermission("user.read")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("user.update")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }
    
    @PostMapping("/{id}/roles")
    @RequiresPermission("user.manage")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable UUID id, @RequestBody Set<UUID> roleIds) {
        return ResponseEntity.ok(userService.assignRoles(id, roleIds));
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("user.manage")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### DEPOIS (Opção 2: Permissão na classe)
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiresPermission("user.read") // Permissão padrão para toda a classe
public class UserController {
    
    @GetMapping // Usa user.read da classe
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(pageable));
    }
    
    @GetMapping("/{id}") // Usa user.read da classe
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("user.update") // Sobrescreve permissão da classe
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }
    
    @PostMapping("/{id}/roles")
    @RequiresPermission("user.manage") // Sobrescreve permissão da classe
    public ResponseEntity<UserResponse> assignRoles(@PathVariable UUID id, @RequestBody Set<UUID> roleIds) {
        return ResponseEntity.ok(userService.assignRoles(id, roleIds));
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("user.manage") // Sobrescreve permissão da classe
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Cenários Avançados

### Cenário 1: Operação que requer múltiplas permissões

```java
@PostMapping("/{id}/publish-and-notify")
@RequiresPermission(value = {"api.publish", "notification.send"}, requireAll = true)
public ResponseEntity<ApiResponse> publishAndNotify(@PathVariable UUID id) {
    // Usuário precisa ter AMBAS as permissões
    return ResponseEntity.ok(apiService.publishAndNotify(id));
}
```

### Cenário 2: Operação que aceita qualquer uma de várias permissões

```java
@GetMapping("/{id}/details")
@RequiresPermission(value = {"api.read", "api.update", "api.delete"}, requireAll = false)
public ResponseEntity<ApiResponse> getDetails(@PathVariable UUID id) {
    // Usuário precisa ter PELO MENOS UMA das permissões
    return ResponseEntity.ok(apiService.getDetails(id));
}
```

### Cenário 3: Verificação programática dentro do método

```java
@Service
@RequiredArgsConstructor
public class ApiService {
    
    private final PermissionService permissionService;
    
    public ApiResponse getApi(UUID id) {
        ApiResponse api = apiRepository.findById(id);
        
        // Mostrar informações sensíveis apenas para quem tem permissão
        if (permissionService.hasPermission("api.manage")) {
            api.setInternalNotes(api.getInternalNotes());
            api.setApiKey(api.getApiKey());
        } else {
            api.setInternalNotes(null);
            api.setApiKey(null);
        }
        
        return api;
    }
}
```

## Checklist de Migração

### Para cada Controller:

- [ ] Identificar todos os `@PreAuthorize`
- [ ] Mapear roles para permissões correspondentes
- [ ] Substituir `@PreAuthorize` por `@RequiresPermission`
- [ ] Testar cada endpoint
- [ ] Verificar logs de acesso negado
- [ ] Atualizar documentação Swagger

### Mapeamento Role → Permissão

| @PreAuthorize | @RequiresPermission |
|---------------|---------------------|
| `hasRole('SUPER_ADMIN')` | Não precisa (SUPER_ADMIN tem tudo) |
| `hasRole('PROVIDER')` | Usar permissão específica (ex: `api.create`) |
| `hasRole('CONSUMER')` | Usar permissão específica (ex: `api.read`) |
| `hasAnyRole('PROVIDER', 'SUPER_ADMIN')` | Usar permissão específica |

## Teste de Migração

### 1. Criar usuário de teste
```sql
-- Criar usuário sem permissões
INSERT INTO users (id, keycloak_id, email, first_name, last_name, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'test-user-123', 'test@example.com', 'Test', 'User', true, NOW(), NOW());
```

### 2. Testar acesso negado
```bash
# Deve retornar 403 Forbidden
curl -X POST http://localhost:8080/api/v1/apis \
  -H "Authorization: Bearer <token_do_test_user>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test API"}'
```

### 3. Atribuir permissão
```sql
-- Criar role de teste
INSERT INTO roles (id, code, name, description, is_system, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'TEST_ROLE', 'Test Role', 'Role para testes', false, true, NOW(), NOW());

-- Atribuir permissão à role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'TEST_ROLE' AND p.code = 'api.create';

-- Atribuir role ao usuário
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'test@example.com' AND r.code = 'TEST_ROLE';
```

### 4. Testar acesso permitido
```bash
# Agora deve funcionar (200 OK)
curl -X POST http://localhost:8080/api/v1/apis \
  -H "Authorization: Bearer <token_do_test_user>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test API"}'
```

## Conclusão

A migração para sistema baseado em permissões oferece:

✅ **Mais controle**: Permissões granulares por operação  
✅ **Mais flexibilidade**: Mudanças sem recompilar  
✅ **Melhor manutenção**: Gerenciamento via banco de dados  
✅ **Melhor auditoria**: Rastreamento claro de permissões  

**Próximo passo**: Migrar todos os controllers seguindo os exemplos acima!
