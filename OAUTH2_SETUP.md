# Configuração de Autenticação OAuth2 (Google, LinkedIn, GitHub)

Este guia explica como configurar autenticação com provedores externos no Keycloak.

## 1. Google OAuth2

### 1.1. Criar Credenciais no Google Cloud Console

1. Acesse: https://console.cloud.google.com/
2. Crie um novo projeto ou selecione um existente
3. Vá em **APIs & Services** → **Credentials**
4. Clique em **Create Credentials** → **OAuth 2.0 Client ID**
5. Configure:
   - Application type: **Web application**
   - Name: `Portal API - Keycloak`
   - Authorized redirect URIs: 
     ```
     http://localhost:8180/realms/portal-api/broker/google/endpoint
     ```
6. Clique em **Create** e copie:
   - Client ID
   - Client Secret

### 1.2. Configurar no Keycloak

1. Acesse o Keycloak Admin Console: http://localhost:8180
2. Login: `admin` / `admin123`
3. Selecione o realm: **portal-api**
4. Vá em **Identity Providers**
5. Clique em **Add provider** → Selecione **Google**
6. Configure:
   - **Alias**: `google` (não altere)
   - **Client ID**: Cole o Client ID do Google
   - **Client Secret**: Cole o Client Secret do Google
   - **Store Tokens**: ON
   - **Trust Email**: ON
   - **First Login Flow**: `first broker login`
7. Clique em **Save**

## 2. LinkedIn OAuth2

### 2.1. Criar App no LinkedIn

1. Acesse: https://www.linkedin.com/developers/apps
2. Clique em **Create app**
3. Preencha os dados da aplicação
4. Em **Auth**, adicione:
   - Redirect URLs:
     ```
     http://localhost:8180/realms/portal-api/broker/linkedin/endpoint
     ```
5. Em **Products**, solicite acesso a:
   - Sign In with LinkedIn
   - Share on LinkedIn
6. Copie:
   - Client ID
   - Client Secret

### 2.2. Configurar no Keycloak

1. No Keycloak Admin Console
2. **Identity Providers** → **Add provider** → **LinkedIn**
3. Configure:
   - **Alias**: `linkedin` (não altere)
   - **Client ID**: Cole o Client ID do LinkedIn
   - **Client Secret**: Cole o Client Secret do LinkedIn
   - **Store Tokens**: ON
   - **Trust Email**: ON
4. Clique em **Save**

## 3. GitHub OAuth2 (Opcional)

### 3.1. Criar OAuth App no GitHub

1. Acesse: https://github.com/settings/developers
2. Clique em **OAuth Apps** → **New OAuth App**
3. Configure:
   - Application name: `Portal API`
   - Homepage URL: `http://localhost:8080`
   - Authorization callback URL:
     ```
     http://localhost:8180/realms/portal-api/broker/github/endpoint
     ```
4. Clique em **Register application**
5. Copie:
   - Client ID
   - Gere e copie o Client Secret

### 3.2. Configurar no Keycloak

1. No Keycloak Admin Console
2. **Identity Providers** → **Add provider** → **GitHub**
3. Configure:
   - **Alias**: `github` (não altere)
   - **Client ID**: Cole o Client ID do GitHub
   - **Client Secret**: Cole o Client Secret do GitHub
   - **Store Tokens**: ON
4. Clique em **Save**

## 4. Configurar Mappers (Opcional)

Para mapear atributos dos provedores para o Keycloak:

1. Em cada Identity Provider, vá na aba **Mappers**
2. Clique em **Add mapper**
3. Exemplos de mappers úteis:
   - **Email**: `email` → `email`
   - **First Name**: `given_name` → `firstName`
   - **Last Name**: `family_name` → `lastName`
   - **Picture**: `picture` → `picture`

## 5. Atribuir Grupos Automaticamente

Para atribuir usuários OAuth2 a grupos automaticamente:

1. Em cada Identity Provider, vá em **Mappers**
2. Clique em **Add mapper**
3. Configure:
   - **Name**: `Default Group`
   - **Mapper Type**: `Hardcoded Attribute`
   - **Attribute Name**: `group`
   - **Attribute Value**: `CONSUMER` (ou outro grupo padrão)

## 6. Testar no Frontend

### 6.1. Obter URLs de autenticação

```bash
GET http://localhost:8080/api/v1/auth/oauth2/providers
```

Resposta:
```json
{
  "google": {
    "name": "Google",
    "authUrl": "http://localhost:8180/realms/portal-api/protocol/openid-connect/auth?..."
  },
  "linkedin": {
    "name": "LinkedIn",
    "authUrl": "http://localhost:8180/realms/portal-api/protocol/openid-connect/auth?..."
  }
}
```

### 6.2. Fluxo de autenticação

1. Frontend redireciona o usuário para `authUrl`
2. Usuário faz login no provedor (Google/LinkedIn)
3. Provedor redireciona para: `http://localhost:3000/auth/callback?code=...`
4. Frontend troca o `code` por token no Keycloak:

```bash
POST http://localhost:8180/realms/portal-api/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&client_id=portal-api
&client_secret=RNEWX0EvC41j5ytpCg9Qw6EuhcEJdixn
&code=CODIGO_RECEBIDO
&redirect_uri=http://localhost:4200/auth/callback
```

5. Keycloak retorna o access_token e refresh_token

## 7. Configuração de Produção

Para produção, atualize as URLs:

### No Google Cloud Console:
```
https://seu-dominio.com/realms/portal-api/broker/google/endpoint
```

### No LinkedIn:
```
https://seu-dominio.com/realms/portal-api/broker/linkedin/endpoint
```

### No .env:
```env
KEYCLOAK_URL=https://keycloak.seu-dominio.com
FRONTEND_URL=https://seu-dominio.com
```

## 8. Endpoints Disponíveis

- `GET /api/v1/auth/oauth2/providers` - Lista todos os provedores configurados
- `GET /api/v1/auth/oauth2/login/{provider}` - Obtém URL de login para um provedor específico

## Troubleshooting

### Erro: "Invalid redirect_uri"
- Verifique se a URL de callback está exatamente igual no provedor e no Keycloak

### Erro: "User not found"
- Verifique se "Trust Email" está ativado no Identity Provider
- Verifique se o First Login Flow está configurado

### Usuário não recebe grupo/role
- Configure mappers no Identity Provider
- Ou crie um Default First Login Flow customizado
