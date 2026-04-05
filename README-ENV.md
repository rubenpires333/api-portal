# Configuração de Variáveis de Ambiente

Este projeto usa o arquivo `.env` para armazenar todas as credenciais e configurações sensíveis.

## Como Funciona

O Spring Boot automaticamente carrega variáveis de ambiente do sistema. Este projeto usa a biblioteca `spring-dotenv` que carrega automaticamente o arquivo `.env` na raiz do projeto.

### Uso Rápido

#### Windows PowerShell
```powershell
# Carregar variáveis e executar
.\load-env.ps1
mvn spring-boot:run
```

#### Linux/Mac
```bash
# Carregar variáveis e executar
source ./load-env.sh
mvn spring-boot:run
```

#### Executar direto (spring-dotenv carrega automaticamente)
```bash
# O spring-dotenv carrega o .env automaticamente ao iniciar
mvn spring-boot:run
```

### Outras Opções

#### IntelliJ IDEA
1. Run > Edit Configurations
2. Selecione sua aplicação Spring Boot
3. Em "Environment variables", clique no ícone de pasta
4. Adicione: `EnvFile` plugin ou copie manualmente as variáveis do `.env`

#### VS Code
Crie `.vscode/launch.json`:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Spring Boot",
            "request": "launch",
            "mainClass": "com.api_portal.backend.BackendApplication",
            "envFile": "${workspaceFolder}/.env"
        }
    ]
}
```

## Setup Inicial

1. Copie o arquivo de exemplo:
```bash
cp .env.example .env
```

2. Edite o `.env` e preencha com suas credenciais reais:
   - Database (PostgreSQL)
   - Keycloak
   - Stripe (https://dashboard.stripe.com/apikeys)
   - Email (opcional)
   - reCAPTCHA (opcional)
   - Vinti4 (opcional)

## Importante

- O arquivo `.env` está no `.gitignore` e NÃO será commitado
- Nunca commite credenciais nos arquivos `.properties`
- Cada desenvolvedor/ambiente deve ter seu próprio `.env`
- Em produção, use variáveis de ambiente do sistema ou secrets management

## Variáveis Obrigatórias

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KEYCLOAK_CLIENT_SECRET`
- `STRIPE_API_KEY`
- `STRIPE_PUBLISHABLE_KEY`
- `STRIPE_WEBHOOK_SECRET`

## Variáveis Opcionais

- `RECAPTCHA_*` - Se `RECAPTCHA_ENABLED=true`
- `MAIL_*` - Se `MAIL_ENABLED=true`
- `VINTI4_*` - Para gateway de pagamento Cabo Verde
