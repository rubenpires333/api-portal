# Correção: Erro CORS no Backend

## Problema

Ao tentar fazer login do frontend (http://localhost:4200) para o backend (http://localhost:8080), ocorria erro CORS:

```
Cross-Origin Request Blocked: The Same Origin Policy disallows reading 
the remote resource at http://localhost:8080/api/v1/auth/login. 
(Reason: CORS header 'Access-Control-Allow-Origin' missing). 
Status code: 403.
```

## Causa

O Spring Security estava bloqueando requisições cross-origin porque não havia configuração de CORS no `SecurityConfig.java`.

## Solução

Adicionada configuração completa de CORS no `SecurityConfig.java`:

### 1. Método `corsConfigurationSource()`

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Permitir origens do frontend
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:4200",
        "http://localhost:3000",
        "http://127.0.0.1:4200"
    ));
    
    // Permitir todos os métodos HTTP
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));
    
    // Permitir todos os headers
    configuration.setAllowedHeaders(Arrays.asList("*"));
    
    // Permitir credenciais (cookies, authorization headers)
    configuration.setAllowCredentials(true);
    
    // Headers expostos para o frontend
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Total-Count"
    ));
    
    // Tempo de cache para preflight requests (1 hora)
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
}
```

### 2. Aplicar CORS no SecurityFilterChain

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        // ... resto da configuração
}
```

## O que foi configurado

### Origens Permitidas (AllowedOrigins)
- `http://localhost:4200` - Frontend Angular (porta padrão)
- `http://localhost:3000` - Alternativa (React, etc)
- `http://127.0.0.1:4200` - Variação do localhost

### Métodos HTTP Permitidos
- GET
- POST
- PUT
- DELETE
- PATCH
- OPTIONS (necessário para preflight requests)

### Headers Permitidos
- Todos (`*`) - Permite qualquer header na requisição

### Credenciais
- `allowCredentials: true` - Permite envio de cookies e headers de autenticação

### Headers Expostos
- `Authorization` - Para tokens JWT
- `Content-Type` - Tipo de conteúdo
- `X-Total-Count` - Para paginação

### Cache de Preflight
- `maxAge: 3600` - Cacheia preflight requests por 1 hora

## Como Funciona

### Requisição Simples (GET, POST sem headers customizados)
```
1. Frontend faz requisição para backend
2. Backend verifica origem no CORS config
3. Se permitida, adiciona header: Access-Control-Allow-Origin: http://localhost:4200
4. Browser permite a resposta
```

### Requisição Preflight (PUT, DELETE, ou com headers customizados)
```
1. Browser envia OPTIONS request (preflight)
2. Backend responde com headers CORS:
   - Access-Control-Allow-Origin: http://localhost:4200
   - Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
   - Access-Control-Allow-Headers: *
   - Access-Control-Max-Age: 3600
3. Browser cacheia resposta por 1 hora
4. Browser envia requisição real
5. Backend processa e responde
```

## Testando

### 1. Reiniciar Backend
```bash
cd backend/api-portal-backend
mvn spring-boot:run
```

### 2. Verificar Logs
Deve aparecer no console:
```
CORS configurado para permitir origens: [http://localhost:4200, http://localhost:3000, http://127.0.0.1:4200]
```

### 3. Testar Login
```bash
# No frontend
cd frontend
ng serve

# Acessar http://localhost:4200/auth/sign-in
# Tentar fazer login
```

### 4. Verificar Headers no Browser
Abrir DevTools > Network > Selecionar requisição > Headers

**Request Headers:**
```
Origin: http://localhost:4200
```

**Response Headers:**
```
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Credentials: true
```

## Produção

Para produção, atualizar as origens permitidas:

```java
configuration.setAllowedOrigins(Arrays.asList(
    "https://seu-dominio.com",
    "https://www.seu-dominio.com"
));
```

Ou usar variável de ambiente:

```java
@Value("${cors.allowed-origins}")
private String allowedOrigins;

// No application.properties
cors.allowed-origins=https://seu-dominio.com,https://www.seu-dominio.com
```

## Segurança

### ✅ Boas Práticas Implementadas
- Origens específicas (não usar `*` em produção)
- `allowCredentials: true` (necessário para cookies/JWT)
- Cache de preflight (reduz requisições OPTIONS)
- Headers expostos limitados

### ⚠️ Atenção
- Nunca usar `allowedOrigins("*")` com `allowCredentials(true)`
- Em produção, listar apenas domínios confiáveis
- Considerar usar padrões regex para subdomínios

## Troubleshooting

### Erro persiste após configuração
1. Limpar cache do browser (Ctrl+Shift+Delete)
2. Reiniciar backend completamente
3. Verificar se porta está correta (4200 vs 3000)

### Erro 403 em OPTIONS request
- Verificar se OPTIONS está em `allowedMethods`
- Verificar se endpoint está em `permitAll()`

### Cookies não sendo enviados
- Verificar `allowCredentials: true`
- Verificar se frontend está usando `withCredentials: true`

### Headers customizados não funcionam
- Verificar se header está em `allowedHeaders`
- Ou usar `allowedHeaders("*")`

---

**Status**: ✅ CORS configurado e funcionando  
**Data**: 28 de Março de 2026
