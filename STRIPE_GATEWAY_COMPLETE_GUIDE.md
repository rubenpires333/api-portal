# 🚀 Guia Completo - Gateway de Pagamento Stripe

## Do Zero à Produção

Este documento explica passo a passo como implementar, configurar, testar e colocar em produção o gateway de pagamento Stripe no API Portal.

---

## 📋 Índice

1. [Visão Geral da Arquitetura](#1-visão-geral-da-arquitetura)
2. [Implementação do Gateway](#2-implementação-do-gateway)
3. [Configuração Local (Desenvolvimento)](#3-configuração-local-desenvolvimento)
4. [Testes Locais](#4-testes-locais)
5. [Preparação para Produção](#5-preparação-para-produção)
6. [Deploy em Produção](#6-deploy-em-produção)
7. [Monitoramento e Manutenção](#7-monitoramento-e-manutenção)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Visão Geral da Arquitetura

### 1.1 Componentes Principais

```
┌─────────────────────────────────────────────────────────┐
│                    PaymentGateway                       │
│                     (Interface)                         │
├─────────────────────────────────────────────────────────┤
│  + getType(): String                                    │
│  + createCheckoutSession(): CheckoutSession             │
│  + parseWebhook(): WebhookEvent                         │
│  + isHealthy(): boolean                                 │
│  + getMetadata(): GatewayMetadata                       │
└─────────────────────────────────────────────────────────┘
                          ▲
                          │
         ┌────────────────┴────────────────┐
         │                                 │
┌────────┴─────────┐            ┌─────────┴────────┐
│  StripeGateway   │            │  Vinti4Gateway   │
│  (Implementação) │            │  (Implementação) │
└──────────────────┘            └──────────────────┘
```

### 1.2 Fluxo de Dados

```
Consumer → Frontend → Backend → StripeGateway → Stripe API
                                      ↓
                                WebhookController
                                      ↓
                                RevenueShareService
                                      ↓
                                WalletService
```

---

## 2. Implementação do Gateway

### 2.1 Estrutura de Arquivos

```
billing/
├── gateway/
│   ├── PaymentGateway.java          ← Interface base
│   ├── PaymentGatewayFactory.java   ← Factory pattern
│   ├── dto/
│   │   ├── CheckoutRequest.java
│   │   ├── CheckoutSession.java
│   │   ├── WebhookEvent.java
│   │   └── GatewayMetadata.java
│   └── stripe/
│       └── StripeGateway.java       ← Implementação Stripe
```

### 2.2 Interface PaymentGateway

Esta interface define o contrato que todos os gateways devem seguir:

```java
public interface PaymentGateway {
    String getType();
    CheckoutSession createCheckoutSession(CheckoutRequest request);
    WebhookEvent parseWebhook(String payload, String signature);
    boolean isHealthy();
    GatewayMetadata getMetadata();
}
```

**Benefícios:**
- ✅ Adicione novos gateways sem modificar código existente
- ✅ Troque de gateway facilmente
- ✅ Teste com mocks
- ✅ Suporte a múltiplos gateways simultâneos

### 2.3 Implementação StripeGateway

Localização: `billing/gateway/stripe/StripeGateway.java`

**Principais Métodos:**

#### createCheckoutSession()
Cria uma sessão de checkout no Stripe para subscriptions.

```java
@Override
public CheckoutSession createCheckoutSession(CheckoutRequest request) {
    SessionCreateParams params = SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .setSuccessUrl(request.getSuccessUrl())
        .setCancelUrl(request.getCancelUrl())
        .addLineItem(...)
        .putAllMetadata(request.getMetadata())
        .build();
    
    Session session = Session.create(params);
    return new CheckoutSession(session.getId(), session.getUrl());
}
```

#### parseWebhook()
Valida e converte webhooks do Stripe.

```java
@Override
public WebhookEvent parseWebhook(String payload, String signature) {
    // Valida assinatura HMAC-SHA256
    Event event = Webhook.constructEvent(payload, signature, webhookSecret);
    
    // Converte para formato interno
    return convertStripeEvent(event);
}
```

---

## 3. Configuração Local (Desenvolvimento)

### 3.1 Pré-requisitos

- ✅ Java 21
- ✅ Maven 3.8+
- ✅ PostgreSQL 16
- ✅ Conta Stripe (gratuita)

### 3.2 Criar Conta Stripe

1. Acesse: https://dashboard.stripe.com/register
2. Preencha os dados (não precisa de cartão)
3. Confirme o email
4. Você estará em **Test Mode** automaticamente

### 3.3 Obter Credenciais de Teste

#### API Keys

1. Acesse: https://dashboard.stripe.com/test/apikeys
2. Copie as chaves:

```
Publishable key: pk_test_51xxxxx... (frontend)
Secret key: sk_test_51xxxxx...      (backend) ⭐
```

#### Criar Produtos e Preços

1. Acesse: https://dashboard.stripe.com/test/products
2. Clique em **"+ Add product"**

**Produto 1: Starter Plan**
- Name: `Starter Plan`
- Price: `$0.00 USD` / month
- Recurring: Monthly
- Copie o **Price ID**: `price_xxxxx`

**Produto 2: Growth Plan**
- Name: `Growth Plan`
- Price: `$49.00 USD` / month
- Recurring: Monthly
- Copie o **Price ID**: `price_xxxxx`

**Produto 3: Business Plan**
- Name: `Business Plan`
- Price: `$149.00 USD` / month
- Recurring: Monthly
- Copie o **Price ID**: `price_xxxxx`

### 3.4 Configurar .env

Edite `api-portal-backend/.env`:

```bash
# Stripe Test Mode
STRIPE_API_KEY=sk_test_51xxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxxxx
STRIPE_PRICE_ID_STARTER=price_xxxxxxxxxxxxxxxxxx
STRIPE_PRICE_ID_GROWTH=price_xxxxxxxxxxxxxxxxxx
STRIPE_PRICE_ID_BUSINESS=price_xxxxxxxxxxxxxxxxxx

# Configurações
PLATFORM_COMMISSION_PERCENTAGE=20.00
HOLDBACK_DAYS=14
AUTO_APPROVE_THRESHOLD=50.00
FRONTEND_URL=http://localhost:4200
```

### 3.5 Instalar Stripe CLI

**Windows:**
```powershell
# Baixar
Invoke-WebRequest -Uri "https://github.com/stripe/stripe-cli/releases/download/v1.19.5/stripe_1.19.5_windows_x86_64.zip" -OutFile "C:\stripe\stripe.zip"

# Extrair
Expand-Archive -Path "C:\stripe\stripe.zip" -DestinationPath "C:\stripe" -Force

# Adicionar ao PATH
$env:Path += ";C:\stripe"
```

**macOS/Linux:**
```bash
brew install stripe/stripe-cli/stripe
```

### 3.6 Autenticar Stripe CLI

```bash
stripe login
```

Pressione Enter e autorize no navegador.

### 3.7 Executar Migração do Banco

```bash
psql -U postgres -d api_portal -f src/main/resources/db/migration/V10__create_billing_tables.sql
```

Isso cria:
- 9 tabelas do billing
- 3 planos padrão
- 5 regras de taxas
- 2 configurações de gateway

---

## 4. Testes Locais

### 4.1 Iniciar Stripe CLI Listener

**Terminal 1:**
```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

Você verá:
```
> Ready! Your webhook signing secret is whsec_xxxxx
```

**Copie o webhook secret** e atualize no `.env`:
```bash
STRIPE_WEBHOOK_SECRET=whsec_xxxxx
```

### 4.2 Iniciar Aplicação

**Terminal 2:**
```bash
cd api-portal-backend
mvn spring-boot:run
```

Aguarde:
```
Started BackendApplication in X.XXX seconds
Registered payment gateways: [STRIPE, VINTI4]
```

### 4.3 Testar Endpoints

#### Teste 1: Consultar Wallet
```bash
curl http://localhost:8080/api/v1/provider/wallet?providerId=123e4567-e89b-12d3-a456-426614174000
```

Resposta esperada:
```json
{
  "availableBalance": 0.00,
  "pendingBalance": 0.00,
  "reservedBalance": 0.00,
  "lifetimeEarned": 0.00,
  "currency": "USD",
  "minimumPayout": 10.00
}
```

#### Teste 2: Criar Checkout Session
```bash
curl -X POST http://localhost:8080/api/v1/billing/checkout/platform \
  -H "Content-Type: application/json" \
  -d '{
    "providerId": "123e4567-e89b-12d3-a456-426614174000",
    "planName": "GROWTH"
  }'
```

Resposta esperada:
```json
{
  "sessionId": "cs_test_xxxxx",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_xxxxx",
  "planName": "Growth",
  "amount": "49.00",
  "currency": "USD"
}
```

### 4.4 Simular Webhooks

**Terminal 3:**
```bash
# Simular pagamento bem-sucedido
stripe trigger invoice.payment_succeeded

# Simular criação de subscription
stripe trigger customer.subscription.created

# Simular checkout completo
stripe trigger checkout.session.completed
```

**Verificar nos logs:**
- Terminal 1 (Stripe CLI): `[200] POST http://localhost:8080/api/v1/webhooks/stripe`
- Terminal 2 (Aplicação): `Webhook received: invoice.payment_succeeded`

### 4.5 Testar com Cartões de Teste

Use estes cartões no checkout:

**Sucesso:**
```
Número: 4242 4242 4242 4242
CVC: 123
Data: 12/34
```

**Falha:**
```
Número: 4000 0000 0000 0002
```

**3D Secure:**
```
Número: 4000 0025 0000 3155
```

---

## 5. Preparação para Produção

### 5.1 Ativar Conta Stripe

1. Acesse: https://dashboard.stripe.com/account/onboarding
2. Complete o formulário:
   - Informações da empresa
   - Dados bancários
   - Documentos de identificação
3. Aguarde aprovação (1-3 dias úteis)

### 5.2 Obter Credenciais de Produção

Após aprovação:

1. Acesse: https://dashboard.stripe.com/apikeys
2. **Desative o Test Mode** (toggle no canto superior direito)
3. Copie as chaves de produção:

```
Secret key: sk_live_51xxxxx...
```

### 5.3 Criar Produtos em Produção

1. Acesse: https://dashboard.stripe.com/products
2. Crie os mesmos 3 produtos (Starter, Growth, Business)
3. Copie os **Price IDs de produção**

### 5.4 Configurar Webhook em Produção

1. Acesse: https://dashboard.stripe.com/webhooks
2. Clique em **"+ Add endpoint"**
3. URL: `https://seu-dominio.com/api/v1/webhooks/stripe`
4. Selecione eventos:
   - `checkout.session.completed`
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
5. Copie o **Signing secret**: `whsec_xxxxx`

### 5.5 Variáveis de Ambiente de Produção

Crie arquivo `.env.production`:

```bash
# Stripe Production
STRIPE_API_KEY=sk_live_51xxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxxxx
STRIPE_PRICE_ID_STARTER=price_xxxxxxxxxxxxxxxxxx
STRIPE_PRICE_ID_GROWTH=price_xxxxxxxxxxxxxxxxxx
STRIPE_PRICE_ID_BUSINESS=price_xxxxxxxxxxxxxxxxxx

# Database Production
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/api_portal
SPRING_DATASOURCE_USERNAME=apiportal_prod
SPRING_DATASOURCE_PASSWORD=SENHA_FORTE_AQUI

# Frontend Production
FRONTEND_URL=https://seu-dominio.com

# Configurações
PLATFORM_COMMISSION_PERCENTAGE=20.00
HOLDBACK_DAYS=14
AUTO_APPROVE_THRESHOLD=50.00
```

### 5.6 Segurança Adicional

#### Encriptar Dados Sensíveis

Configure Jasypt no `application-prod.properties`:

```properties
jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}
jasypt.encryptor.algorithm=PBEWITHHMACSHA512ANDAES_256
jasypt.encryptor.key-obtention-iterations=1000
```

#### SSL/TLS Obrigatório

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

---

## 6. Deploy em Produção

### 6.1 Build da Aplicação

```bash
# Compilar com profile de produção
mvn clean package -Pprod -DskipTests

# Gerar JAR
# Arquivo gerado: target/api-portal-backend-0.0.1-SNAPSHOT.jar
```

### 6.2 Deploy com Docker

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - STRIPE_API_KEY=${STRIPE_API_KEY}
      - STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}
    depends_on:
      - postgres
  
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: api_portal
      POSTGRES_USER: apiportal_prod
      POSTGRES_PASSWORD: ${DB_PASSWORD}
```

**Deploy:**
```bash
docker-compose up -d
```

### 6.3 Deploy em Cloud (AWS/Azure/GCP)

#### AWS Elastic Beanstalk
```bash
eb init -p docker api-portal-backend
eb create prod-env
eb deploy
```

#### Azure App Service
```bash
az webapp create --resource-group api-portal --plan prod-plan --name api-portal-backend
az webapp deployment source config-zip --resource-group api-portal --name api-portal-backend --src target/api-portal-backend.jar
```

#### Google Cloud Run
```bash
gcloud run deploy api-portal-backend \
  --image gcr.io/PROJECT_ID/api-portal-backend \
  --platform managed \
  --region us-central1
```

---

## 7. Monitoramento e Manutenção

### 7.1 Logs Importantes

```bash
# Ver logs do billing
tail -f logs/spring.log | grep billing

# Ver webhooks recebidos
tail -f logs/spring.log | grep WebhookController

# Ver jobs agendados
tail -f logs/spring.log | grep HoldbackReleaseScheduler
```

### 7.2 Métricas no Stripe Dashboard

Monitore em tempo real:

- **Pagamentos**: https://dashboard.stripe.com/payments
- **Subscriptions**: https://dashboard.stripe.com/subscriptions
- **Webhooks**: https://dashboard.stripe.com/webhooks
- **Logs**: https://dashboard.stripe.com/logs

### 7.3 Queries de Monitoramento

**Revenue Total:**
```sql
SELECT SUM(platform_commission) as total_revenue
FROM revenue_share_events
WHERE created_at >= NOW() - INTERVAL '30 days';
```

**Saldo em Wallets:**
```sql
SELECT 
    SUM(available_balance) as total_available,
    SUM(pending_balance) as total_pending,
    COUNT(*) as total_wallets
FROM provider_wallets;
```

**Levantamentos Pendentes:**
```sql
SELECT COUNT(*), SUM(requested_amount)
FROM withdrawal_requests
WHERE status = 'PENDING_APPROVAL';
```

### 7.4 Alertas Recomendados

Configure alertas para:

- ✅ Webhook com status 4xx ou 5xx
- ✅ Pagamento falhou
- ✅ Saldo da wallet negativo
- ✅ Job de holdback não executou
- ✅ Gateway health check falhou

---

## 8. Troubleshooting

### 8.1 Webhook Retorna 400

**Causa:** Assinatura inválida

**Solução:**
```bash
# Verificar webhook secret
echo $STRIPE_WEBHOOK_SECRET

# Recriar webhook no Stripe Dashboard
# Copiar novo signing secret
```

### 8.2 Checkout Session Falha

**Causa:** Price ID inválido

**Solução:**
```bash
# Verificar price IDs no .env
# Confirmar que existem no Stripe Dashboard
stripe prices list
```

### 8.3 Job de Holdback Não Executa

**Causa:** Scheduler desabilitado

**Solução:**
```java
// Verificar se @EnableScheduling está presente
@Configuration
@EnableScheduling
public class BillingConfig { }
```

### 8.4 Gateway Not Found

**Causa:** Gateway não registrado

**Solução:**
```sql
-- Verificar configuração no banco
SELECT * FROM gateway_configs WHERE active = true;

-- Ativar Stripe
UPDATE gateway_configs SET active = true WHERE gateway_type = 'STRIPE';
```

---

## 📚 Recursos Adicionais

- [Stripe API Reference](https://stripe.com/docs/api)
- [Stripe Webhooks Guide](https://stripe.com/docs/webhooks)
- [Stripe Testing](https://stripe.com/docs/testing)
- [Stripe Security](https://stripe.com/docs/security)
- [Stripe Best Practices](https://stripe.com/docs/development/best-practices)

---

## ✅ Checklist Final

### Desenvolvimento
- [ ] Conta Stripe criada
- [ ] API keys obtidas
- [ ] Produtos criados
- [ ] .env configurado
- [ ] Stripe CLI instalado
- [ ] Migração executada
- [ ] Aplicação rodando
- [ ] Webhooks testados

### Produção
- [ ] Conta Stripe ativada
- [ ] Credenciais de produção obtidas
- [ ] Produtos de produção criados
- [ ] Webhook de produção configurado
- [ ] SSL/TLS configurado
- [ ] Variáveis de ambiente de produção
- [ ] Deploy realizado
- [ ] Monitoramento configurado
- [ ] Alertas configurados
- [ ] Backup configurado

---

**Criado em:** 04 de Abril de 2026  
**Versão:** 1.0.0  
**Status:** Completo e Testado
