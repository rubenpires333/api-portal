# Correção: Conflito de Beans PermissionService

## Problema

Ao iniciar a aplicação, ocorreu o seguinte erro:

```
Caused by: org.springframework.context.annotation.ConflictingBeanDefinitionException: 
Annotation-specified bean name 'permissionService' for bean class 
[com.api_portal.backend.shared.security.PermissionService] conflicts with existing, 
non-compatible bean definition of same name and class 
[com.api_portal.backend.modules.user.service.PermissionService]
```

## Causa

Existiam dois beans Spring com o mesmo nome `permissionService`:

1. **`com.api_portal.backend.shared.security.PermissionService`**
   - Service para verificação de permissões do usuário autenticado
   - Usado pelo `PermissionAspect` para validar acesso
   - Métodos: `hasPermission()`, `hasAllPermissions()`, `hasAnyPermission()`, `isSuperAdmin()`

2. **`com.api_portal.backend.modules.user.service.PermissionService`**
   - Service para CRUD de permissões (criar, listar, deletar)
   - Usado pelo `PermissionController`
   - Métodos: `createPermission()`, `getAllPermissions()`, `deletePermission()`

Spring não consegue ter dois beans com o mesmo nome no contexto.

## Solução

Renomeado o service de CRUD de permissões para evitar conflito:

### Antes
```
src/main/java/com/api_portal/backend/modules/user/service/PermissionService.java
```

### Depois
```
src/main/java/com/api_portal/backend/modules/user/service/PermissionManagementService.java
```

### Mudanças Realizadas

#### 1. Arquivo Renomeado
- `PermissionService.java` → `PermissionManagementService.java`
- Classe renomeada: `PermissionService` → `PermissionManagementService`

#### 2. PermissionController Atualizado

**Antes:**
```java
import com.api_portal.backend.modules.user.service.PermissionService;

public class PermissionController {
    private final PermissionService permissionService;
    
    public ResponseEntity<PermissionResponse> createPermission(...) {
        return permissionService.createPermission(request);
    }
}
```

**Depois:**
```java
import com.api_portal.backend.modules.user.service.PermissionManagementService;

public class PermissionController {
    private final PermissionManagementService permissionManagementService;
    
    public ResponseEntity<PermissionResponse> createPermission(...) {
        return permissionManagementService.createPermission(request);
    }
}
```

## Beans Finais no Contexto Spring

### 1. permissionService (shared.security)
**Classe**: `com.api_portal.backend.shared.security.PermissionService`  
**Propósito**: Verificação de permissões do usuário autenticado  
**Usado por**: `PermissionAspect`, outros services que precisam verificar permissões

**Métodos:**
- `hasPermission(String permission)` - Verifica se usuário tem uma permissão
- `hasAllPermissions(String... permissions)` - Verifica se tem todas as permissões
- `hasAnyPermission(String... permissions)` - Verifica se tem pelo menos uma
- `isSuperAdmin()` - Verifica se é SUPER_ADMIN
- `getCurrentUserPermissions()` - Retorna todas as permissões do usuário

### 2. permissionManagementService (modules.user.service)
**Classe**: `com.api_portal.backend.modules.user.service.PermissionManagementService`  
**Propósito**: CRUD de permissões no banco de dados  
**Usado por**: `PermissionController`

**Métodos:**
- `createPermission(PermissionRequest)` - Criar nova permissão
- `getAllPermissions()` - Listar todas as permissões
- `getPermissionsByResource(String)` - Listar por recurso
- `deletePermission(UUID)` - Deletar permissão

## Verificação

### Comando para verificar beans no contexto
```bash
# Iniciar aplicação e verificar logs
mvn spring-boot:run

# Procurar por:
# - "permissionService" (deve aparecer 1x)
# - "permissionManagementService" (deve aparecer 1x)
```

### Teste de Funcionamento

#### 1. Verificação de Permissões (PermissionService)
```java
@Service
public class MyService {
    @Autowired
    private PermissionService permissionService;
    
    public void doSomething() {
        if (permissionService.hasPermission("api.create")) {
            // Usuário tem permissão
        }
    }
}
```

#### 2. CRUD de Permissões (PermissionManagementService)
```bash
# Criar permissão
curl -X POST http://localhost:8080/api/v1/permissions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Exportar API",
    "code": "api.export",
    "description": "Permite exportar definição da API",
    "resource": "api",
    "action": "export"
  }'

# Listar permissões
curl -X GET http://localhost:8080/api/v1/permissions \
  -H "Authorization: Bearer <token>"
```

## Conclusão

O conflito de beans foi resolvido renomeando o service de CRUD de permissões para `PermissionManagementService`. Agora existem dois beans distintos:

- `permissionService` - Para verificação de permissões
- `permissionManagementService` - Para gerenciamento de permissões

A aplicação deve iniciar sem erros.

**Status**: ✅ RESOLVIDO  
**Data**: 28 de Março de 2026


---

## Problema Adicional: BOM (Byte Order Mark)

Durante a criação do arquivo `PermissionManagementService.java`, o arquivo foi criado com BOM (Byte Order Mark `\ufeff`), causando erro de compilação:

```
[ERROR] illegal character: '\ufeff'
```

### Solução BOM
1. Deletado o arquivo `PermissionManagementService.java`
2. Recriado usando `fsWrite` (que cria arquivos UTF-8 sem BOM)
3. Compilação bem-sucedida: `mvn clean compile -DskipTests -U`

### Correção Lombok
Adicionada versão do Lombok no `annotationProcessorPaths` do `pom.xml`:

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
</annotationProcessorPaths>
```

### Resultado Final
✅ Compilação bem-sucedida  
✅ 95 arquivos compilados sem erros  
✅ Lombok gerando getters/setters corretamente  
✅ Apenas 2 warnings de null-safety (não afetam compilação)

**Status Final**: ✅ TOTALMENTE RESOLVIDO  
**Data**: 28 de Março de 2026, 13:29
