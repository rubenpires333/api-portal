# Módulo API - Planejamento de Implementação

## 🎯 Objetivo

Criar um sistema completo de gerenciamento de APIs onde:
- **Provedores** podem registrar e gerenciar suas APIs
- **Consumidores** podem descobrir e consumir APIs
- **Administradores** podem aprovar e monitorar APIs

## 📊 Estrutura do Módulo

```
src/main/java/com/api_portal/backend/modules/api/
├── controller/
│   ├── ApiController.java           # CRUD de APIs
│   ├── ApiVersionController.java    # Gerenciar versões
│   └── ApiCategoryController.java   # Categorias de APIs
├── domain/
│   ├── Api.java                     # Entidade principal
│   ├── ApiVersion.java              # Versões da API
│   ├── ApiEndpoint.java             # Endpoints da API
│   ├── ApiCategory.java             # Categorias
│   └── enums/
│       ├── ApiStatus.java           # DRAFT, PUBLISHED, DEPRECATED
│       ├── ApiVisibility.java       # PUBLIC, PRIVATE, INTERNAL
│       └── AuthType.java            # API_KEY, OAUTH2, BASIC
├── dto/
│   ├── ApiRequest.java
│   ├── ApiResponse.java
│   ├── ApiVersionRequest.java
│   └── ApiEndpointRequest.java
├── repository/
│   ├── ApiRepository.java
│   ├── ApiVersionRepository.java
│   ├── ApiEndpointRepository.java
│   └── ApiCategoryRepository.java
├── service/
│   ├── ApiService.java
│   ├── ApiVersionService.java
│   └── ApiCategoryService.java
└── exception/
    └── ApiException.java
```

## 🗄️ Modelo de Dados

### 1. API (Entidade Principal)

```java
@Entity
@Table(name = "apis")
public class Api {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;                    // Nome da API
    private String slug;                    // URL-friendly name
    private String description;             // Descrição
    private String shortDescription;        // Descrição curta
    
    @ManyToOne
    private ApiCategory category;           // Categoria
    
    @Enumerated(EnumType.STRING)
    private ApiStatus status;               // DRAFT, PUBLISHED, DEPRECATED
    
    @Enumerated(EnumType.STRING)
    private ApiVisibility visibility;       // PUBLIC, PRIVATE, INTERNAL
    
    private String providerId;              // ID do provedor (usuário)
    private String providerName;            // Nome do provedor
    private String providerEmail;           // Email do provedor
    
    private String baseUrl;                 // URL base da API
    private String documentationUrl;        // URL da documentação
    private String termsOfServiceUrl;       // Termos de serviço
    
    @Enumerated(EnumType.STRING)
    private AuthType authType;              // Tipo de autenticação
    
    private String logoUrl;                 // Logo da API
    private String iconUrl;                 // Ícone
    
    @ElementCollection
    private List<String> tags;              // Tags para busca
    
    private Integer rateLimit;              // Limite de requisições
    private String rateLimitPeriod;         // Período (hour, day, month)
    
    private Boolean requiresApproval;       // Requer aprovação para uso
    private Boolean isActive;               // Ativa/Inativa
    
    @OneToMany(mappedBy = "api", cascade = CascadeType.ALL)
    private List<ApiVersion> versions;      // Versões
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
```

### 2. ApiVersion (Versões)

```java
@Entity
@Table(name = "api_versions")
public class ApiVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    private Api api;
    
    private String version;                 // v1, v2, v1.0.0
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ApiStatus status;
    
    private Boolean isDefault;              // Versão padrão
    private Boolean isDeprecated;
    private LocalDateTime deprecatedAt;
    private String deprecationMessage;
    
    private String baseUrl;                 // URL específica da versão
    private String openApiSpec;             // Spec OpenAPI/Swagger (JSON)
    
    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL)
    private List<ApiEndpoint> endpoints;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3. ApiEndpoint (Endpoints)

```java
@Entity
@Table(name = "api_endpoints")
public class ApiEndpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    private ApiVersion version;
    
    private String path;                    // /users/{id}
    private String method;                  // GET, POST, PUT, DELETE
    private String summary;                 // Resumo
    private String description;             // Descrição detalhada
    
    @ElementCollection
    private List<String> tags;
    
    private Boolean requiresAuth;
    private Boolean isDeprecated;
    
    @Column(columnDefinition = "TEXT")
    private String requestExample;          // JSON exemplo
    
    @Column(columnDefinition = "TEXT")
    private String responseExample;         // JSON exemplo
    
    private Integer responseTime;           // Tempo médio (ms)
    private Double successRate;             // Taxa de sucesso (%)
}
```

### 4. ApiCategory (Categorias)

```java
@Entity
@Table(name = "api_categories")
public class ApiCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;                    // Pagamentos, Autenticação, etc
    private String slug;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    
    @OneToMany(mappedBy = "category")
    private List<Api> apis;
}
```

## 🔐 Permissões e Autorização

### Roles e Permissões

```java
// SUPER_ADMIN
- Criar/Editar/Deletar qualquer API
- Aprovar/Rejeitar APIs
- Gerenciar categorias
- Ver estatísticas globais

// PROVIDER
- Criar/Editar/Deletar suas próprias APIs
- Publicar APIs (com ou sem aprovação)
- Ver estatísticas das suas APIs
- Gerenciar versões e endpoints

// CONSUMER
- Ver APIs públicas
- Solicitar acesso a APIs privadas
- Ver documentação
- Testar endpoints (sandbox)
```

## 📡 Endpoints da API

### APIs

```
GET    /api/v1/apis                    # Listar APIs (públicas + autorizadas)
GET    /api/v1/apis/{id}               # Detalhes da API
POST   /api/v1/apis                    # Criar API (PROVIDER)
PUT    /api/v1/apis/{id}               # Atualizar API (PROVIDER/ADMIN)
DELETE /api/v1/apis/{id}               # Deletar API (PROVIDER/ADMIN)
PATCH  /api/v1/apis/{id}/publish       # Publicar API
PATCH  /api/v1/apis/{id}/deprecate     # Depreciar API
GET    /api/v1/apis/my                 # Minhas APIs (PROVIDER)
GET    /api/v1/apis/search              # Buscar APIs
```

### Versões

```
GET    /api/v1/apis/{apiId}/versions           # Listar versões
POST   /api/v1/apis/{apiId}/versions           # Criar versão
PUT    /api/v1/apis/{apiId}/versions/{id}      # Atualizar versão
DELETE /api/v1/apis/{apiId}/versions/{id}      # Deletar versão
PATCH  /api/v1/apis/{apiId}/versions/{id}/default  # Definir como padrão
```

### Endpoints

```
GET    /api/v1/versions/{versionId}/endpoints      # Listar endpoints
POST   /api/v1/versions/{versionId}/endpoints      # Criar endpoint
PUT    /api/v1/versions/{versionId}/endpoints/{id} # Atualizar endpoint
DELETE /api/v1/versions/{versionId}/endpoints/{id} # Deletar endpoint
```

### Categorias

```
GET    /api/v1/categories              # Listar categorias
POST   /api/v1/categories              # Criar categoria (ADMIN)
PUT    /api/v1/categories/{id}         # Atualizar categoria (ADMIN)
DELETE /api/v1/categories/{id}         # Deletar categoria (ADMIN)
```

## 🎨 Features Principais

### 1. Descoberta de APIs
- Busca por nome, descrição, tags
- Filtro por categoria
- Filtro por tipo de autenticação
- Ordenação por popularidade, data, nome

### 2. Documentação Automática
- Importar OpenAPI/Swagger spec
- Gerar documentação interativa
- Exemplos de request/response
- Playground para testar endpoints

### 3. Versionamento
- Múltiplas versões por API
- Versão padrão
- Deprecação de versões antigas
- Migração entre versões

### 4. Controle de Acesso
- APIs públicas (sem autenticação)
- APIs privadas (requer aprovação)
- APIs internas (apenas organização)

### 5. Monitoramento
- Estatísticas de uso
- Tempo de resposta
- Taxa de sucesso/erro
- Endpoints mais usados

## 🚀 Ordem de Implementação

### Fase 1: Estrutura Básica (Prioridade Alta)
1. ✅ Criar entidades (Api, ApiVersion, ApiEndpoint, ApiCategory)
2. ✅ Criar repositories
3. ✅ Criar DTOs
4. ✅ Criar services básicos
5. ✅ Criar controllers CRUD

### Fase 2: Features Essenciais (Prioridade Alta)
1. ✅ Sistema de busca e filtros
2. ✅ Versionamento de APIs
3. ✅ Categorização
4. ✅ Controle de permissões (PROVIDER/CONSUMER/ADMIN)

### Fase 3: Features Avançadas (Prioridade Média)
1. ⏳ Importar OpenAPI/Swagger spec
2. ⏳ Documentação interativa
3. ⏳ Sistema de aprovação
4. ⏳ Rate limiting

### Fase 4: Monitoramento (Prioridade Baixa)
1. ⏳ Estatísticas de uso
2. ⏳ Analytics
3. ⏳ Logs de acesso

## 📝 Próximos Passos

1. Criar as entidades do domínio
2. Criar os repositories
3. Criar os DTOs
4. Implementar os services
5. Criar os controllers
6. Adicionar validações
7. Testes unitários
8. Documentação Swagger

## 🔗 Integração com Módulo Auth

- Usar `@PreAuthorize` para controle de acesso
- Extrair `userId` do JWT para associar APIs ao provedor
- Validar roles (PROVIDER, CONSUMER, SUPER_ADMIN)

```java
@PreAuthorize("hasRole('PROVIDER')")
@PostMapping
public ResponseEntity<ApiResponse> createApi(@RequestBody ApiRequest request) {
    // Extrair userId do JWT
    String providerId = getCurrentUserId();
    return apiService.createApi(request, providerId);
}
```

Deseja que eu comece a implementar o Módulo API?
