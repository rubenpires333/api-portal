# Integração: Tela de Registro (Sign-Up)

## Data
28 de Março de 2026

## Implementação Completa

### Frontend - signup.component.ts

**Funcionalidades:**
- Formulário reativo com validação
- Campos: firstName, lastName, email, password, confirmPassword, acceptTerms
- Validação de senha mínima (6 caracteres)
- Validação de senhas coincidentes
- Integração com AuthenticationService
- Login social (Google e GitHub)
- Redirecionamento automático após registro

**Validações:**
```typescript
{
  firstName: ['', Validators.required],
  lastName: [''],
  email: ['', [Validators.required, Validators.email]],
  password: ['', [Validators.required, Validators.minLength(6)]],
  confirmPassword: ['', Validators.required],
  acceptTerms: [false, Validators.requiredTrue]
}
```

**Validador Customizado:**
```typescript
passwordMatchValidator(group: FormGroup) {
  const password = group.get('password')?.value
  const confirmPassword = group.get('confirmPassword')?.value
  return password === confirmPassword ? null : { passwordMismatch: true }
}
```

### Frontend - signup.component.html

**Características:**
- Design consistente com tela de login
- Mensagens de erro inline
- Feedback visual (is-invalid)
- Loading state no botão
- Botões de login social
- Link para tela de login

### Backend - RegisterRequest.java

**Campos Aceitos:**
```java
{
  "firstName": "João",           // Opcional
  "lastName": "Silva",            // Opcional
  "name": "João Silva",           // Alternativa a firstName/lastName
  "email": "joao@example.com",    // Obrigatório
  "password": "password123",      // Obrigatório (mín. 6 caracteres)
  "role": "CONSUMER"              // Opcional (padrão: CONSUMER)
}
```

**Métodos Auxiliares:**
- `getFullName()` - Retorna nome completo de firstName/lastName ou name
- `getRoleOrDefault()` - Retorna role ou CONSUMER como padrão

### Backend - KeycloakAdminService.java

**Método createUser Atualizado:**
- Aceita firstName e lastName separados
- Fallback para name se firstName não fornecido
- Fallback para email se nenhum nome fornecido
- Role padrão: CONSUMER
- Cria usuário no Keycloak
- Atribui role automaticamente
- Adiciona usuário ao grupo correspondente

## Fluxo de Registro

### 1. Usuário Preenche Formulário

```
Frontend:
- Nome: João
- Sobrenome: Silva
- Email: joao@example.com
- Senha: password123
- Confirmar Senha: password123
- ✓ Aceito os Termos
```

### 2. Frontend Valida e Envia

```typescript
POST /api/v1/auth/register
Body:
{
  "firstName": "João",
  "lastName": "Silva",
  "email": "joao@example.com",
  "password": "password123"
}
```

### 3. Backend Cria Usuário no Keycloak

```
1. Obter token admin do Keycloak
2. Criar usuário:
   - username: joao@example.com
   - email: joao@example.com
   - firstName: João
   - lastName: Silva
   - enabled: true
   - emailVerified: false
   - credentials: [password]
3. Atribuir role CONSUMER
4. Adicionar ao grupo CONSUMER
```

### 4. Backend Faz Login Automático

```
1. Chamar método login() com email e senha
2. Obter tokens do Keycloak
3. Sincronizar usuário no banco de dados
4. Retornar tokens e informações do usuário
```

### 5. Frontend Armazena Sessão

```typescript
{
  accessToken: "eyJhbGciOiJSUzI1NiIs...",
  refreshToken: "eyJhbGciOiJIUzI1NiIs...",
  expiresIn: 300,
  user: {
    id: "keycloak-user-id",
    email: "joao@example.com",
    username: "joao@example.com",
    firstName: "João",
    lastName: "Silva",
    roles: ["CONSUMER"]
  }
}
```

### 6. Redirecionamento

```typescript
this.router.navigate(['/'])  // Redireciona para dashboard
```

## Registro com Login Social

### Google

```typescript
loginWithGoogle() {
  // Redireciona para Keycloak com kc_idp_hint=google
  window.location.href = 
    'http://localhost:8180/realms/portal-api/protocol/openid-connect/auth?...'
}
```

**Fluxo:**
1. Usuário clica em "Cadastrar com Google"
2. Redirecionado para Google OAuth
3. Google autentica e retorna código
4. Callback processa código
5. Usuário criado automaticamente no Keycloak
6. Sincronizado no banco de dados
7. Redirecionado para dashboard

### GitHub

```typescript
loginWithGithub() {
  // Redireciona para Keycloak com kc_idp_hint=github
  window.location.href = 
    'http://localhost:8180/realms/portal-api/protocol/openid-connect/auth?...'
}
```

**Fluxo:** Idêntico ao Google, mas com GitHub OAuth

## Validações

### Frontend

| Campo | Validação | Mensagem |
|-------|-----------|----------|
| firstName | required | "Nome é obrigatório" |
| email | required, email | "Email é obrigatório" / "Email inválido" |
| password | required, minLength(6) | "Senha é obrigatória" / "Senha deve ter no mínimo 6 caracteres" |
| confirmPassword | required, passwordMatch | "Confirmação de senha é obrigatória" / "As senhas não coincidem" |
| acceptTerms | requiredTrue | "Você deve aceitar os termos e condições" |

### Backend

| Campo | Validação | Mensagem |
|-------|-----------|----------|
| email | @NotBlank, @Email | "Email é obrigatório" / "Email inválido" |
| password | @NotBlank, @Size(min=6) | "Password é obrigatória" / "Password deve ter no mínimo 6 caracteres" |

## Tratamento de Erros

### Email Já Existe

**Backend:**
```java
throw new AuthException("Email já está em uso ou erro ao criar utilizador");
```

**Frontend:**
```typescript
error: (error) => {
  this.error = error.error?.message || 'Erro ao criar conta. Tente novamente.'
}
```

**Exibição:**
```html
<div class="alert alert-danger">
  Email já está em uso ou erro ao criar utilizador
</div>
```

### Senha Fraca

**Keycloak pode rejeitar senhas fracas se configurado:**
- Mínimo de caracteres
- Letras maiúsculas/minúsculas
- Números
- Caracteres especiais

### Erro de Conexão

**Frontend:**
```typescript
error: (error) => {
  if (error.status === 0) {
    this.error = 'Erro de conexão. Verifique se o servidor está rodando.'
  }
}
```

## Testando

### 1. Registro Manual

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "João",
    "lastName": "Silva",
    "email": "joao@example.com",
    "password": "password123"
  }'
```

**Resposta esperada:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "user": {
    "id": "keycloak-user-id",
    "email": "joao@example.com",
    "username": "joao@example.com",
    "firstName": "João",
    "lastName": "Silva",
    "roles": ["CONSUMER"]
  }
}
```

### 2. Verificar Usuário no Keycloak

1. Acessar: http://localhost:8180/admin
2. Login: admin / admin
3. Realm: portal-api
4. Users > View all users
5. Verificar: joao@example.com

### 3. Verificar Usuário no Banco

```sql
SELECT * FROM users WHERE email = 'joao@example.com';
SELECT * FROM user_roles WHERE user_id = (SELECT id FROM users WHERE email = 'joao@example.com');
```

### 4. Testar Login Após Registro

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "password": "password123"
  }'
```

## Configuração de Roles

### Roles Disponíveis

```java
public enum UserRole {
    SUPER_ADMIN,  // Administrador do sistema
    PROVIDER,     // Provedor de APIs
    CONSUMER      // Consumidor de APIs (padrão)
}
```

### Registro com Role Específica

**Apenas para testes ou admin:**
```json
{
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@example.com",
  "password": "admin123",
  "role": "SUPER_ADMIN"
}
```

**Produção:** Sempre usar CONSUMER por padrão. Admins devem ser criados manualmente.

## Melhorias Futuras

### 1. Verificação de Email

```java
// Enviar email de verificação após registro
keycloakAdminService.sendVerificationEmail(userId);
```

### 2. Captcha

```html
<re-captcha (resolved)="onCaptchaResolved($event)"></re-captcha>
```

### 3. Força da Senha

```typescript
getPasswordStrength(password: string): 'weak' | 'medium' | 'strong' {
  // Implementar lógica de força da senha
}
```

### 4. Validação de Email em Tempo Real

```typescript
emailExists(email: string): Observable<boolean> {
  return this.http.get<boolean>(`${apiUrl}/auth/email-exists?email=${email}`)
}
```

### 5. Termos e Condições

```html
<a routerLink="/terms" target="_blank">Termos e Condições</a>
```

## Arquivos Modificados

### Frontend
- `frontend/src/app/views/auth/signup/signup.component.ts`
- `frontend/src/app/views/auth/signup/signup.component.html`

### Backend
- `api-portal-backend/src/main/java/com/api_portal/backend/modules/auth/dto/RegisterRequest.java`
- `api-portal-backend/src/main/java/com/api_portal/backend/modules/auth/service/KeycloakAdminService.java`

## Checklist

- [x] Formulário de registro criado
- [x] Validações implementadas
- [x] Integração com backend
- [x] Login automático após registro
- [x] Suporte a firstName/lastName
- [x] Role padrão CONSUMER
- [x] Login social (Google e GitHub)
- [x] Tratamento de erros
- [x] Redirecionamento após sucesso
- [ ] Verificação de email
- [ ] Captcha
- [ ] Força da senha
- [ ] Termos e condições

---

**Status**: ✅ Registro funcionando completamente  
**Próximo**: Testar registro end-to-end e implementar verificação de email
