# Sistema de Controle de Acesso Baseado em Permissões

## Visão Geral

O sistema agora usa **permissões** ao invés de **roles** para controlar acesso aos endpoints. Isso oferece:

✅ **Flexibilidade**: Altere permissões sem recompilar  
✅ **Granularidade**: Controle fino sobre cada operação  
✅ **Escalabilidade**: Fácil adicionar novas permissões  
✅ **Manutenção**: Gerenciamento via banco de dados  

## Componentes

### 1. @RequiresPermission
Anotação para marcar métodos/classes que requerem permissões específicas.

### 2. PermissionService
Service para verificar permissões do usuário autenticado.

### 3. PermissionAspect
Aspect que intercepta chamadas e valida permissões automaticamente.

## Como Usar

### Exemplo 1: Permissão Única

```java
@RestController
@RequestMapping("/api/v1/apis")
public class ApiController {
    
    @PostMapping
    @RequiresPermission("api.create")
    public ResponseEntity<ApiResponse> createApi(@RequestBody ApiRequest request) {
        // Apenas usuários com permissão "api.create" podem acessar
        return ResponseEntity.ok(apiService.create(request));
    }
}
```

### Exemplo 2: Múltiplas Permissões (Requer TODAS)

```java
@PutMapping("/{id}")
@RequiresPermission(value = {"api.update", "api.publish"}, requireAll = true)
public ResponseEntity<ApiResponse> updateAndPublish(@PathVariable UUID id) {
    // Usuário precisa ter AMBAS as permissões
    return ResponseEntity.ok(apiService.updateAndPublish(id));
}
```

### Exemplo 3: Múltiplas Permissões (Requer PELO MENOS UMA)

```java
@GetMapping("/{id}")
@RequiresPermission(value = {"api.read", "api.update"}, requireAll = false)
public ResponseEntity<ApiResponse> getApi(@PathVariable UUID id) {
    // Usuário precisa ter api.read OU api.update
    return ResponseEntity.ok(apiService.getById(id));
}
```

### Exemplo 4: Permissão na Classe (Aplica a TODOS os métodos)

```java
@RestController
@RequestMapping("/api/v1/admin")
@RequiresPermission("user.manage")
public class AdminController {
    
    // Todos os métodos desta classe requerem "user.manage"
    
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAll());
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Exemplo 5: Sobrescrever Permissão da Classe

```java
@RestController
@RequestMapping("/api/v1/apis")
@RequiresPermission("api.read") // Permissão padrão da classe
public class ApiController {
    
    @GetMapping // Usa permissão da classe: api.read
    public ResponseEntity<List<ApiResponse>> getAll() {
        return ResponseEntity.ok(apiService.getAll());
    }
    
    @PostMapping
    @RequiresPermission("api.create") // Sobrescreve permissão da classe
    public ResponseEntity<ApiResponse> create(@RequestBody ApiRequest request) {
        return ResponseEntity.ok(apiService.create(request));
    }
}
```

### Exemplo 6: Mensagem de Erro Customizada

```java
@DeleteMapping("/{id}")
@RequiresPermission(
    value = "api.delete",
    message = "Você não tem permissão para deletar APIs. Entre em contato com o administrador."
)
public ResponseEntity<Void> deleteApi(@PathVariable UUID id) {
    apiService.delete(id);
    return ResponseEntity.noContent().build();
}
```

## Permissões Padrão do Sistema

### APIs
- `api.create` - Criar APIs
- `api.read` - Visualizar APIs
- `api.update` - Atualizar APIs
- `api.delete` - Deletar APIs
- `api.publish` - Publicar APIs

### Categorias
- `category.create` - Criar categorias
- `category.read` - Visualizar categorias
- `category.update` - Atualizar categorias
- `category.delete` - Deletar categorias

### Usuários
- `user.manage` - Gerenciar usuários
- `user.read` - Visualizar usuários
- `user.update` - Atualizar usuários

### Roles
- `role.manage` - Gerenciar roles
- `role.read` - Visualizar roles

### Permissões
- `permission.manage` - Gerenciar permissões
- `permission.read` - Visualizar permissões

### Auditoria
- `audit.read` - Visualizar logs de auditoria

### Versões
- `version.create` - Criar versões de API
- `version.read` - Visualizar versões
- `version.update` - Atualizar versões
- `version.delete` - Deletar versões

### Endpoints
- `endpoint.create` - Criar endpoints
- `endpoint.read` - Visualizar endpoints
- `endpoint.update` - Atualizar endpoints
- `endpoint.delete` - Deletar endpoints

## Roles e Suas Permissões

### SUPER_ADMIN
Tem TODAS as 26 permissões. Sempre tem acesso a tudo.

### PROVIDER
- `api.create`, `api.read`, `api.update`, `api.delete`, `api.publish`
- `category.read`
- `user.read`

### CONSUMER
- `api.read`
- `category.read`
- `user.read`

## PermissionService - Uso Programático

Se precisar verificar permissões manualmente no código:

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final PermissionService permissionService;
    
    public void doSomething() {
        // Verificar uma permissão
        if (permissionService.hasPermission("api.create")) {
            // Usuário tem permissão
        }
        
        // Verificar múltiplas (TODAS)
        if (permissionService.hasAllPermissions("api.create", "api.publish")) {
            // Usuário tem ambas
        }
        
        // Verificar múltiplas (PELO MENOS UMA)
        if (permissionService.hasAnyPermission("api.read", "api.update")) {
            // Usuário tem pelo menos uma
        }
        
        // Verificar se é SUPER_ADMIN
        if (permissionService.isSuperAdmin()) {
            // Usuário é SUPER_ADMIN
        }
        
        // Obter todas as permissões do usuário
        Set<String> permissions = permissionService.getCurrentUserPermissions();
    }
}
```

## Migração de @PreAuthorize para @RequiresPermission

### Antes (usando roles)
```java
@PreAuthorize("hasRole('PROVIDER')")
@PostMapping
public ResponseEntity<ApiResponse> createApi(@RequestBody ApiRequest request) {
    return ResponseEntity.ok(apiService.create(request));
}
```

### Depois (usando permissões)
```java
@RequiresPermission("api.create")
@PostMapping
public ResponseEntity<ApiResponse> createApi(@RequestBody ApiRequest request) {
    return ResponseEntity.ok(apiService.create(request));
}
```

## Vantagens da Migração

### Antes
- Para adicionar nova permissão, precisava criar nova role ou modificar código
- Difícil gerenciar permissões granulares
- Acoplamento entre código e roles

### Depois
- Adicione permissões via banco de dados
- Atribua permissões a roles dinamicamente
- Sem necessidade de recompilar
- Controle fino sobre cada operação

## Exemplo Completo: ApiController

```java
@RestController
@RequestMapping("/api/v1/apis")
@RequiredArgsConstructor
@Tag(name = "APIs", description = "Gerenciamento de APIs")
public class ApiController {
    
    private final ApiService apiService;
    
    // Público - sem permissão necessária
    @GetMapping
    @Operation(summary = "Listar todas as APIs públicas")
    public ResponseEntity<Page<ApiResponse>> getAllApis(Pageable pageable) {
        return ResponseEntity.ok(apiService.getAllApis(pageable));
    }
    
    // Requer permissão de leitura
    @GetMapping("/{id}")
    @RequiresPermission("api.read")
    @Operation(summary = "Obter detalhes de uma API")
    public ResponseEntity<ApiResponse> getApiById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiService.getApiById(id));
    }
    
    // Requer permissão de criação
    @PostMapping
    @RequiresPermission("api.create")
    @Operation(summary = "Criar nova API")
    public ResponseEntity<ApiResponse> createApi(@RequestBody ApiRequest request) {
        return ResponseEntity.ok(apiService.create(request));
    }
    
    // Requer permissão de atualização
    @PutMapping("/{id}")
    @RequiresPermission("api.update")
    @Operation(summary = "Atualizar API")
    public ResponseEntity<ApiResponse> updateApi(
            @PathVariable UUID id,
            @RequestBody ApiRequest request) {
        return ResponseEntity.ok(apiService.update(id, request));
    }
    
    // Requer permissão de publicação
    @PostMapping("/{id}/publish")
    @RequiresPermission("api.publish")
    @Operation(summary = "Publicar API")
    public ResponseEntity<ApiResponse> publishApi(@PathVariable UUID id) {
        return ResponseEntity.ok(apiService.publish(id));
    }
    
    // Requer permissão de deleção
    @DeleteMapping("/{id}")
    @RequiresPermission("api.delete")
    @Operation(summary = "Deletar API")
    public ResponseEntity<Void> deleteApi(@PathVariable UUID id) {
        apiService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Gerenciamento de Permissões

### Adicionar Nova Permissão

```sql
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Exportar API',
    'api.export',
    'Permite exportar definição da API',
    'api',
    'export',
    true,
    NOW(),
    NOW()
);
```

### Atribuir Permissão a uma Role

```sql
-- Obter IDs
SELECT id FROM roles WHERE code = 'PROVIDER';      -- role_id
SELECT id FROM permissions WHERE code = 'api.export'; -- permission_id

-- Atribuir
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('role_id_aqui', 'permission_id_aqui');
```

### Remover Permissão de uma Role

```sql
DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM roles WHERE code = 'PROVIDER')
  AND permission_id = (SELECT id FROM permissions WHERE code = 'api.export');
```

## Logs e Debug

O sistema gera logs detalhados:

```
DEBUG - Verificando permissões: api.create (requireAll=true)
DEBUG - Usuário é SUPER_ADMIN, acesso permitido
```

```
WARN  - Usuário não possui permissão: api.delete
WARN  - Acesso negado ao método: ApiController.deleteApi - Permissões necessárias: api.delete
```

## Tratamento de Erros

Quando acesso é negado, o sistema lança `AccessDeniedException`:

```json
{
  "timestamp": "2026-03-28T13:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Acesso negado: permissão insuficiente",
  "path": "/api/v1/apis"
}
```

## Próximos Passos

- [ ] Migrar todos os controllers de `@PreAuthorize` para `@RequiresPermission`
- [ ] Criar interface de gerenciamento de permissões no frontend
- [ ] Implementar cache de permissões para performance
- [ ] Adicionar auditoria de verificações de permissão
- [ ] Criar relatório de uso de permissões por usuário
