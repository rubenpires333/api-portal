# Migração para Variáveis de Ambiente (.env)

## O que mudou?

Todas as credenciais e configurações sensíveis foram movidas do `application.properties` e `application-local.properties` para o arquivo `.env`.

## Arquivos Modificados

### 1. `application.properties`
- Removidos valores hardcoded de secrets
- Todas as propriedades agora usam `${VARIAVEL_ENV:valor_padrao}`

### 2. `application-billing.properties`
- Configurações do Stripe agora vêm do `.env`
- Configurações do Vinti4 agora vêm do `.env`

### 3. `.env` (novo formato)
Agora contém TODAS as credenciais:
- Database (PostgreSQL)
- Keycloak
- Stripe (API Key, Publishable Key, Webhook Secret)
- Email (opcional)
- reCAPTCHA (opcional)
- Vinti4 (opcional)
- Configurações de billing

### 4. `pom.xml`
Adicionada dependência `spring-dotenv` para carregar automaticamente o `.env`

## Como Usar

### Setup Inicial
```bash
# 1. Copie o arquivo de exemplo
cp .env.example .env

# 2. Edite o .env com suas credenciais reais
# Especialmente: STRIPE_API_KEY, STRIPE_PUBLISHABLE_KEY, STRIPE_WEBHOOK_SECRET

# 3. Execute a aplicação
mvn spring-boot:run
```

### Scripts Auxiliares

#### Windows
```powershell
.\load-env.ps1  # Carrega variáveis no terminal atual
mvn spring-boot:run
```

#### Linux/Mac
```bash
source ./load-env.sh  # Carrega variáveis no terminal atual
mvn spring-boot:run
```

## Vantagens

1. ✅ Secrets não são mais commitados no git
2. ✅ Fácil configuração por ambiente (dev, staging, prod)
3. ✅ Compatível com Docker e CI/CD
4. ✅ Cada desenvolvedor tem suas próprias credenciais
5. ✅ Segue as melhores práticas de segurança

## Variáveis Obrigatórias

Certifique-se de configurar no `.env`:

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/db_portal_api
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha

# Keycloak
KEYCLOAK_CLIENT_SECRET=seu_client_secret
KEYCLOAK_ADMIN_PASSWORD=sua_senha_admin

# Stripe (OBRIGATÓRIO para billing)
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRICE_ID_STARTER=price_...
STRIPE_PRICE_ID_GROWTH=price_...
STRIPE_PRICE_ID_BUSINESS=price_...
```

## Troubleshooting

### Erro: "Could not resolve placeholder"
- Verifique se o `.env` existe na raiz do projeto
- Verifique se a variável está definida no `.env`
- Execute `mvn clean install` para recarregar dependências

### Stripe não funciona
- Verifique se `STRIPE_API_KEY` começa com `sk_test_` ou `sk_live_`
- Verifique se `STRIPE_PUBLISHABLE_KEY` começa com `pk_test_` ou `pk_live_`
- Verifique se os Price IDs estão corretos no Stripe Dashboard

### IDE não carrega o .env
- Use os scripts `load-env.ps1` ou `load-env.sh` antes de executar
- Ou configure as variáveis de ambiente na configuração de execução da IDE
- Veja `README-ENV.md` para instruções específicas da IDE

## Produção

Em produção, NÃO use o arquivo `.env`. Configure as variáveis de ambiente diretamente no sistema:

- AWS: Use AWS Systems Manager Parameter Store ou Secrets Manager
- Azure: Use Azure Key Vault
- Google Cloud: Use Secret Manager
- Docker: Use `docker-compose.yml` com `env_file` ou variáveis de ambiente
- Kubernetes: Use ConfigMaps e Secrets

## Segurança

- ⚠️ NUNCA commite o arquivo `.env`
- ⚠️ Revogue e regenere as chaves do Stripe após qualquer exposição
- ⚠️ Use chaves de teste (`sk_test_`) em desenvolvimento
- ⚠️ Use chaves de produção (`sk_live_`) apenas em produção
