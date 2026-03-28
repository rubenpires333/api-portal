# Data Initializer - Guia de Uso

## Visão Geral

O `DataInitializerService` é um serviço que executa automaticamente ao iniciar a aplicação e garante que todas as permissões e roles padrão do sistema estejam criadas no banco de dados.

## Características

- Executa automaticamente ao iniciar a aplicação
- Verifica se permissões já existem antes de criar (idempotente)
- Cria roles do sistema com permissões associadas
- Suporta permissões customizadas via `application.properties`
- Logs detalhados de todas as operações
- Transacional (rollback em caso de erro)

## Como Funciona

### 1. Inicialização Automática

O serviço implementa `CommandLineRunner`, executando após a aplicação iniciar:

```
1. Aplicação inicia
2. Spring Boot carrega todos os beans
3. DataInitializerService.run() é executado
4. Permissões são verificadas/criadas
5. Roles são verificadas/criadas
6. Aplicação fica pronta para uso
```

### 2. Verificação de Permissões

Para cada permissão padrão:
- Verifica se já existe pelo código
- Se não existir, cria a permissão
- Se existir, apenas carrega para associar às roles

### 3. Verificação de Roles

Para cada role do sistema:
- Verifica se já existe pelo código
- Se não existir, cria a role com permissões associadas
- Se existir, não faz nada (mantém configuração atual)

## Permissões Padrão

### APIs
- `api.create` - Criar API
- `api.read` - Ler API
- `api.update` - Atualizar API
- `api.delete` - Deletar API
- `api.publish` - Publicar API

### Categorias
- `category.create` - Criar Categoria
- `category.read` - Ler Categoria
- `category.update` - Atualizar Categoria
- `category.delete` - Deletar Categoria

### Usuários
- `user.manage` - Gerenciar Usuários
- `user.read` - Ler Usuário
- `user.update` - Atualizar Usuário

### Roles
- `role.manage` - Gerenciar Roles
- `role.read` - Ler Role

### Permissões
- `permission.manage` - Gerenciar Permissões
- `permission.read` - Ler Permissão

### Auditoria
- `audit.read` - Ler Auditoria

### Versões
- `version.create` - Criar Versão
- `version.read` - Ler Versão
- `version.update` - Atualizar Versão
- `version.delete` - Deletar Versão

### Endpoints
- `endpoint.create` - Criar Endpoint
- `endpoint.read` - Ler Endpoint
- `endpoint.update` - Atualizar Endpoint
- `endpoint.delete` - Deletar Endpoint

## Roles Padrão

### SUPER_ADMIN
- Todas as permissões
- Role do sistema (não pode ser deletada)
- Acesso total ao sistema

### PROVIDER
- `api.*` - Todas as permissões de API
- `category.read` - Visualizar categorias
- `user.read` - Visualizar usuários

### CONSUMER
- `api.read` - Visualizar APIs
- `category.read` - Visualizar categorias
- `user.read` - Visualizar usuários

## Adicionar Permissões Customizadas

### Via application.properties

Adicione no `application.properties`:

```properties
# Permissão customizada 1
app.permissions.custom[0].name=Exportar Relatório
app.permissions.custom[0].code=report.export
app.permissions.custom[0].description=Permite exportar relatórios
app.permissions.custom[0].resource=report
app.permissions.custom[0].action=export

# Permissão customizada 2
app.permissions.custom[1].name=Aprovar Subscrição
app.permissions.custom[1].code=subscription.approve
app.permissions.custom[1].description=Permite aprovar subscrições
app.permissions.custom[1].resource=subscription
app.permissions.custom[1].action=approve

# Permissão customizada 3
app.permissions.custom[2].name=Ver Analytics
app.permissions.custom[2].code=analytics.view
app.permissions.custom[2].description=Permite visualizar analytics
app.permissions.custom[2].resource=analytics
app.permissions.custom[2].action=view
```

### Via Código

Edite o método `getDefaultPermissions()` em `DataInitializerService.java`:

```java
private List<PermissionData> getDefaultPermissions() {
    List<PermissionData> permissions = new ArrayList<>();
    
    // ... permissões existentes ...
    
    // Adicionar nova permissão
    permissions.add(new PermissionData(
        "Exportar Relatório", 
        "report.export", 
        "Permite exportar relatórios", 
        "report", 
        "export"
    ));
    
    return permissions;
}
```

## Logs de Inicialização

Ao iniciar a aplicação, você verá logs como:

```
=== Iniciando verificação de dados do sistema ===
Verificando permissões do sistema...
✓ Permissão criada: Criar API (api.create)
✓ Permissão criada: Ler API (api.read)
...
✓ 26 novas permissões criadas
Total de permissões no sistema: 26

Verificando roles do sistema...
✓ Role criada: SUPER_ADMIN com 26 permissões
✓ Role criada: PROVIDER com 7 permissões
✓ Role criada: CONSUMER com 3 permissões
Total de roles no sistema: 3

=== Verificação de dados concluída ===
```

Se as permissões já existirem:

```
=== Iniciando verificação de dados do sistema ===
Verificando permissões do sistema...
Total de permissões no sistema: 26

Verificando roles do sistema...
✓ Role SUPER_ADMIN já existe
✓ Role PROVIDER já existe
✓ Role CONSUMER já existe
Total de roles no sistema: 3

=== Verificação de dados concluída ===
```

## Adicionar Nova Role

Para adicionar uma nova role padrão, edite o método `initializeRoles()`:

```java
// API_MANAGER
if (!roleRepository.existsByCode("API_MANAGER")) {
    Set<Permission> apiManagerPerms = getPermissionsByCode(allPermissions,
        "api.create", "api.read", "api.update", "api.publish",
        "category.read", "category.create", "category.update"
    );
    
    Role apiManager = Role.builder()
        .name("API Manager")
        .code("API_MANAGER")
        .description("Gerencia APIs e categorias")
        .isSystem(false) // false = pode ser deletada
        .active(true)
        .permissions(apiManagerPerms)
        .build();
    
    roleRepository.save(apiManager);
    log.info("✓ Role criada: API_MANAGER com {} permissões", apiManagerPerms.size());
} else {
    log.info("✓ Role API_MANAGER já existe");
}
```

## Padrão de Nomenclatura

### Códigos de Permissão
Formato: `resource.action`

Exemplos:
- `api.create`
- `user.manage`
- `report.export`
- `subscription.approve`

### Códigos de Role
Formato: `UPPER_SNAKE_CASE`

Exemplos:
- `SUPER_ADMIN`
- `PROVIDER`
- `API_MANAGER`
- `CONTENT_MODERATOR`

## Recursos Sugeridos

Para novos módulos, use estes recursos:

- `subscription` - Subscrições
- `analytics` - Analytics
- `billing` - Cobrança
- `notification` - Notificações
- `report` - Relatórios
- `webhook` - Webhooks
- `apikey` - API Keys
- `documentation` - Documentação

## Ações Sugeridas

- `create` - Criar
- `read` - Ler/Visualizar
- `update` - Atualizar
- `delete` - Deletar
- `manage` - Gerenciar (todas as operações)
- `publish` - Publicar
- `approve` - Aprovar
- `reject` - Rejeitar
- `export` - Exportar
- `import` - Importar
- `view` - Visualizar (apenas leitura)
- `execute` - Executar

## Boas Práticas

1. **Sempre use códigos únicos**: Verifique se o código não existe antes de adicionar
2. **Seja descritivo**: Use nomes claros e descrições detalhadas
3. **Agrupe por recurso**: Mantenha permissões do mesmo recurso juntas
4. **Use padrão consistente**: Sempre `resource.action`
5. **Documente mudanças**: Adicione comentários ao adicionar novas permissões
6. **Teste após adicionar**: Reinicie a aplicação e verifique os logs

## Troubleshooting

### Permissões não são criadas

1. Verificar logs da aplicação
2. Verificar se há erros de conexão com banco
3. Verificar se as tabelas existem
4. Verificar transação não foi revertida

### Permissões duplicadas

1. Verificar se o código é único
2. Limpar banco e reiniciar aplicação
3. Verificar logs para identificar duplicatas

### Role não tem permissões

1. Verificar se as permissões existem
2. Verificar se os códigos estão corretos em `getPermissionsByCode()`
3. Verificar logs de criação da role

### Aplicação não inicia

1. Verificar se o banco está acessível
2. Verificar se as tabelas foram criadas
3. Verificar logs de erro
4. Comentar `@Service` temporariamente para debug

## Desabilitar Inicialização

Se precisar desabilitar temporariamente:

### Opção 1: Comentar @Service

```java
//@Service
@RequiredArgsConstructor
public class DataInitializerService implements CommandLineRunner {
```

### Opção 2: Adicionar Condição

```java
@Service
@ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataInitializerService implements CommandLineRunner {
```

Depois adicione no `application.properties`:

```properties
# Desabilitar inicialização de dados
app.data-initializer.enabled=false
```

## Exemplo Completo

### 1. Adicionar Permissões no application.properties

```properties
# Módulo de Relatórios
app.permissions.custom[0].name=Criar Relatório
app.permissions.custom[0].code=report.create
app.permissions.custom[0].description=Permite criar relatórios
app.permissions.custom[0].resource=report
app.permissions.custom[0].action=create

app.permissions.custom[1].name=Exportar Relatório
app.permissions.custom[1].code=report.export
app.permissions.custom[1].description=Permite exportar relatórios
app.permissions.custom[1].resource=report
app.permissions.custom[1].action=export
```

### 2. Criar Role Customizada

Adicione no `initializeRoles()`:

```java
// REPORT_MANAGER
if (!roleRepository.existsByCode("REPORT_MANAGER")) {
    Set<Permission> reportPerms = getPermissionsByCode(allPermissions,
        "report.create", "report.export", "analytics.view"
    );
    
    Role reportManager = Role.builder()
        .name("Report Manager")
        .code("REPORT_MANAGER")
        .description("Gerencia relatórios do sistema")
        .isSystem(false)
        .active(true)
        .permissions(reportPerms)
        .build();
    
    roleRepository.save(reportManager);
    log.info("✓ Role criada: REPORT_MANAGER");
}
```

### 3. Reiniciar Aplicação

```bash
mvn spring-boot:run
```

### 4. Verificar Logs

```
✓ Permissão criada: Criar Relatório (report.create)
✓ Permissão criada: Exportar Relatório (report.export)
✓ Role criada: REPORT_MANAGER com 3 permissões
```

## Próximos Passos

- [ ] Adicionar suporte para atualização de permissões existentes
- [ ] Implementar versionamento de permissões
- [ ] Adicionar UI para gerenciar permissões
- [ ] Implementar importação/exportação de permissões
- [ ] Adicionar validação de códigos duplicados
