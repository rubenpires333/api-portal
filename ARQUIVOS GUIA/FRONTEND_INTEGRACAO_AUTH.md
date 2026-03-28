# Integração Frontend - Autenticação com Keycloak e Google

## Visão Geral

Sistema de autenticação integrado com:
- Login tradicional (email/senha)
- Keycloak OAuth2
- Google OAuth2
- Controle de acesso baseado em roles e permissões

## Estrutura Criada

### 1. Arquivos de Configuração

#### `frontend/src/environments/environment.ts`
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'apicv',
    clientId: 'api-portal-frontend'
  },
  google: {
    clientId: 'YOUR_GOOGLE_CLIENT_ID' // Substituir
  }
};
```

### 2. Serviços

#### `AuthenticationService` (`core/services/auth.service.ts`)

**Métodos principais:**
- `login(email, password)` - Login tradicional
- `loginWithKeycloak()` - Redireciona para Keycloak
- `loginWithGoogle()` - Redireciona para Google
- `handleKeycloakCallback(code)` - Processa retorno do Keycloak
- `handleGoogleCallback(code)` - Processa retorno do Google
- `register(userData)` - Registro de novo usuário
- `refreshToken()` - Renovar token expirado
- `logout()` - Fazer logout
- `isAuthenticated()` - Verificar se está autenticado
- `hasPermission(permission)` - Verificar permissão
- `hasRole(role)` - Verificar role

### 3. Componentes

#### `SigninComponent` (`views/auth/signin/`)
- Tela de login inspirada no API Brasil
- Formulário com email e senha
- Botões para login social (Google, Keycloak)
- Validação de formulário
- Tratamento de erros
- Loading states

#### `AuthCallbackComponent` (`views/auth/callback/`)
- Processa retorno do OAuth2
- Extrai código de autorização
- Chama backend para trocar código por token
- Redireciona baseado na role do usuário

### 4. Guards

#### `authGuard` (`core/guards/auth.guard.ts`)
- Protege rotas que exigem autenticação
- Redireciona para login se não autenticado

#### `roleGuard`
- Protege rotas baseado em roles
- Uso: `canActivate: [roleGuard], data: { roles: ['ADMIN'] }`

#### `permissionGuard`
- Protege rotas baseado em permissões
- Uso: `canActivate: [permissionGuard], data: { permissions: ['api.create'] }`

### 5. Interceptors

#### `authInterceptor` (`core/interceptors/auth.interceptor.ts`)
- Adiciona token JWT automaticamente em todas as requisições
- Trata erros 401 (não autenticado) - faz logout
- Trata erros 403 (sem permissão) - mostra mensagem

## Fluxo de Autenticação

### Login Tradicional

```
1. Usuário preenche email/senha
2. Frontend chama POST /api/v1/auth/login
3. Backend valida credenciais
4. Backend retorna { access_token, refresh_token, user }
5. Frontend salva tokens e dados do usuário
6. Frontend redireciona baseado na role
```

### Login com Keycloak

```
1. Usuário clica em "Login com Keycloak"
2. Frontend redireciona para Keycloak
   URL: http://localhost:8180/realms/apicv/protocol/openid-connect/auth
3. Usuário faz login no Keycloak
4. Keycloak redireciona para /auth/callback?code=XXX
5. Frontend extrai código e chama POST /api/v1/auth/oauth2/keycloak
6. Backend troca código por token com Keycloak
7. Backend retorna { access_token, refresh_token, user }
8. Frontend salva tokens e redireciona
```

### Login com Google

```
1. Usuário clica em "Login com Google"
2. Frontend redireciona para Google OAuth
   URL: https://accounts.google.com/o/oauth2/v2/auth
3. Usuário autoriza no Google
4. Google redireciona para /auth/callback/google?code=XXX
5. Frontend extrai código e chama POST /api/v1/auth/oauth2/google
6. Backend troca código por token com Google
7. Backend retorna { access_token, refresh_token, user }
8. Frontend salva tokens e redireciona
```

## Redirecionamento Baseado em Roles

```typescript
if (roles.includes('SUPER_ADMIN') || roles.includes('ADMIN')) {
  router.navigate(['/module/admin/dashboard']);
} else if (roles.includes('PROVIDER')) {
  router.navigate(['/module/provider/dashboard']);
} else if (roles.includes('CONSUMER')) {
  router.navigate(['/module/consumer/dashboard']);
} else {
  router.navigate(['/']);
}
```

## Estrutura de Módulos

```
frontend/src/app/module/
├── admin/          # Módulo para ADMIN e SUPER_ADMIN
│   ├── dashboard/
│   ├── users/
│   ├── apis/
│   └── permissions/
├── provider/       # Módulo para PROVIDER (quem publica APIs)
│   ├── dashboard/
│   ├── my-apis/
│   └── analytics/
└── consumer/       # Módulo para CONSUMER (quem consome APIs)
    ├── dashboard/
    ├── api-catalog/
    └── my-subscriptions/
```

## Configuração Necessária

### 1. Keycloak

Criar client no Keycloak:
```
Client ID: api-portal-frontend
Client Protocol: openid-connect
Access Type: public
Valid Redirect URIs: http://localhost:4200/auth/callback*
Web Origins: http://localhost:4200
```

### 2. Google OAuth

1. Acessar [Google Cloud Console](https://console.cloud.google.com/)
2. Criar projeto
3. Ativar Google+ API
4. Criar credenciais OAuth 2.0
5. Adicionar URI de redirecionamento: `http://localhost:4200/auth/callback/google`
6. Copiar Client ID e atualizar `environment.ts`

### 3. Backend

Endpoints necessários no backend:
- `POST /api/v1/auth/login` - Login tradicional
- `POST /api/v1/auth/register` - Registro
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/oauth2/keycloak` - Callback Keycloak
- `POST /api/v1/auth/oauth2/google` - Callback Google

## Uso de Guards nas Rotas

```typescript
// Rota protegida (apenas autenticados)
{
  path: 'dashboard',
  component: DashboardComponent,
  canActivate: [authGuard]
}

// Rota protegida por role
{
  path: 'admin',
  loadChildren: () => import('./module/admin/admin.routes'),
  canActivate: [roleGuard],
  data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
}

// Rota protegida por permissão
{
  path: 'apis/create',
  component: CreateApiComponent,
  canActivate: [permissionGuard],
  data: { permissions: ['api.create'] }
}
```

## Testando

### 1. Iniciar Backend
```bash
cd backend/api-portal-backend
mvn spring-boot:run
```

### 2. Iniciar Frontend
```bash
cd frontend
npm start
# ou
ng serve
```

### 3. Acessar
```
http://localhost:4200/auth/sign-in
```

### 4. Testar Login
- Email: admin@apicv.com
- Senha: admin123

## Próximos Passos

1. ✅ Tela de login criada
2. ✅ Integração com Keycloak configurada
3. ✅ Integração com Google configurada
4. ✅ Guards e interceptors criados
5. ⏳ Criar módulos admin/provider/consumer
6. ⏳ Criar dashboards para cada role
7. ⏳ Implementar catálogo de APIs
8. ⏳ Implementar gerenciamento de usuários
9. ⏳ Implementar sistema de permissões no frontend

## Troubleshooting

### Erro: "Cannot find module '@core/...'"

Verificar `tsconfig.json`:
```json
{
  "compilerOptions": {
    "paths": {
      "@core/*": ["src/app/core/*"],
      "@component/*": ["src/app/components/*"],
      "@store/*": ["src/app/store/*"]
    }
  }
}
```

### Erro: CORS

Adicionar no backend `SecurityConfig.java`:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### Token não está sendo enviado

Verificar se o interceptor está registrado em `app.config.ts`:
```typescript
provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
```

---

**Status**: ✅ Integração básica completa  
**Data**: 28 de Março de 2026
