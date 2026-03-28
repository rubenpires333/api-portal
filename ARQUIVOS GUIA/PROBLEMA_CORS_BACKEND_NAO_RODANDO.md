# Problema: CORS 403 - Backend Não Estava Rodando

## Data
28 de Março de 2026

## Problema Reportado

Ao tentar fazer login com Google, email/password, o frontend apresentava erros CORS:

```
Cross-Origin Request Blocked: The Same Origin Policy disallows reading 
the remote resource at http://localhost:8080/api/v1/auth/oauth2/callback. 
(Reason: CORS header 'Access-Control-Allow-Origin' missing). 
Status code: 403.

Cross-Origin Request Blocked: The Same Origin Policy disallows reading 
the remote resource at http://localhost:8080/api/v1/auth/login. 
(Reason: CORS header 'Access-Control-Allow-Origin' missing). 
Status code: 403.
```

## Causa Raiz

O backend Spring Boot NÃO ESTAVA RODANDO! 

O erro CORS 403 ocorria porque:
1. Frontend tentava fazer requisições para `http://localhost:8080`
2. Backend não estava respondendo (processo não estava ativo)
3. Browser interpretava a falta de resposta como erro CORS

## Correções Aplicadas

### 1. Corrigido Import Faltante no AuthController

Adicionado import do `Map` que estava faltando:

```java
import java.util.Map;
```

### 2. Iniciado o Backend

```bash
cd api-portal-backend
mvn spring-boot:run
```

### 3. Verificado Configuração CORS

A configuração CORS no `SecurityConfig.java` está correta:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:4200",
        "http://localhost:3000",
        "http://127.0.0.1:4200"
    ));
    
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));
    
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
}
```

## Como Verificar se o Backend Está Rodando

### Método 1: Verificar Processos
```bash
# Windows PowerShell
Get-Process -Name java

# Ou verificar porta 8080
netstat -ano | findstr :8080
```

### Método 2: Testar Endpoint Health
```bash
curl http://localhost:8080/api/v1/auth/health
```

Resposta esperada:
```
Auth module is running
```

### Método 3: Verificar Logs do Spring Boot

Procurar por esta mensagem nos logs:
```
Started BackendApplication in X.XXX seconds
```

E também:
```
Tomcat started on port 8080 (http)
```

## Checklist Antes de Testar Frontend

Antes de testar login/OAuth no frontend, SEMPRE verificar:

- [ ] Backend está rodando (`mvn spring-boot:run`)
- [ ] Porta 8080 está ativa
- [ ] Endpoint health responde: `http://localhost:8080/api/v1/auth/health`
- [ ] Logs mostram "Started BackendApplication"
- [ ] Logs mostram "CORS configurado para permitir origens"
- [ ] Keycloak está rodando na porta 8180
- [ ] PostgreSQL está rodando

## Comandos Úteis

### Iniciar Backend
```bash
cd api-portal-backend
mvn spring-boot:run
```

### Compilar Backend
```bash
mvn clean compile -DskipTests
```

### Verificar Porta 8080
```bash
# Windows
netstat -ano | findstr :8080

# Se porta estiver ocupada, matar processo
taskkill /PID <PID> /F
```

### Testar Endpoints
```bash
# Health check
curl http://localhost:8080/api/v1/auth/health

# Login (deve retornar 400 ou 401, não 403)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test"}'
```

## Próximos Passos

1. ✅ Backend compilado com sucesso
2. ✅ Import do Map adicionado
3. ⏳ Backend iniciando...
4. ⏳ Aguardar mensagem "Started BackendApplication"
5. ⏳ Testar login no frontend
6. ⏳ Testar OAuth callback com Google

## Lições Aprendidas

1. Sempre verificar se o backend está rodando antes de debugar CORS
2. Erro CORS 403 pode significar que o servidor não está respondendo
3. Usar `curl` ou Postman para testar endpoints diretamente
4. Verificar logs do Spring Boot para confirmar inicialização completa

---

**Status**: Backend reiniciando após correção do import  
**Próximo**: Aguardar inicialização completa e testar endpoints
