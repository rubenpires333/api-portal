# Guia Completo: Configuração e Teste do Sistema de Billing Admin

## 📋 Visão Geral

Este guia cobre o setup completo do sistema de billing, desde a migração do banco de dados até os testes dos endpoints de administração.

---

## 🗄️ PASSO 1: Executar Migração do Banco de Dados

A migração `V10__create_billing_tables.sql` cria todas as tabelas necessárias para o sistema de billing.

### Opção A: Migração Automática (Recomendado)

O Flyway executará automaticamente ao iniciar a aplicação:

```bash
cd api-portal-backend
mvn spring-boot:run -DskipTests
```

### Opção B: Migração Manual

Se preferir executar manualmente:

```bash
psql -U apiportal -d db_portal_api -f src/main/resources/db/migration/V10__create_billing_tables.sql
```

### Verificar Migração

```sql
-- Conectar ao banco
psql -U apiportal -d db_portal_api

-- Verificar tabelas criadas
\dt billing_*

-- Deve mostrar:
-- billing_gateway_configs
-- billing_platform_plans
-- billing_provider_wallets
-- billing_wallet_transactions
-- billing_revenue_share_events
-- billing_withdrawal_requests
-- billing_withdrawal_fee_rules
-- billing_payment_webhooks
```

---

## 💳 PASSO 2: Criar Produtos no Stripe Dashboard

### 2.1 Acessar Stripe Dashboard

1. Acesse: https://dashboard.stripe.com/test/products
2. Login com sua conta Stripe

### 2.2 Criar Produto "Starter Plan"

1. Clique em **"+ Add product"**
2. Preencha:
   - **Name**: `Starter Plan`
   - **Description**: `Free plan for testing APIs`
   - **Pricing model**: `Standard pricing`
   - **Price**: `0.00 USD`
   - **Billing period**: `Monthly`
3. Clique em **"Save product"**
4. **COPIE o Price ID** (formato: `price_xxxxxxxxxxxxx`)

### 2.3 Criar Produto "Growth Plan"

1. Clique em **"+ Add product"**
2. Preencha:
   - **Name**: `Growth Plan`
   - **Description**: `Professional plan with advanced features`
   - **Pricing model**: `Standard pricing`
   - **Price**: `49.00 USD`
   - **Billing period**: `Monthly`
3. Clique em **"Save product"**
4. **COPIE o Price ID**

### 2.4 Criar Produto "Business Plan"

1. Clique em **"+ Add product"**
2. Preencha:
   - **Name**: `Business Plan`
   - **Description**: `Enterprise plan with unlimited features`
   - **Pricing model**: `Standard pricing`
   - **Price**: `149.00 USD`
   - **Billing period**: `Monthly`
3. Clique em **"Save product"**
4. **COPIE o Price ID**

### 2.5 Atualizar .env com Price IDs

Edite o arquivo `.env` e substitua os valores:

```env
STRIPE_PRICE_ID_STARTER=price_1234567890abcdef  # Cole o ID real aqui
STRIPE_PRICE_ID_GROWTH=price_abcdef1234567890   # Cole o ID real aqui
STRIPE_PRICE_ID_BUSINESS=price_fedcba0987654321 # Cole o ID real aqui
```

### 2.6 Reiniciar Aplicação

```bash
# Parar a aplicação (Ctrl+C)
# Reiniciar
mvn spring-boot:run -DskipTests
```

---

## 🔧 PASSO 3: Configurar Gateway Stripe via API

### 3.1 Obter Token de Autenticação

Primeiro, faça login como SUPERADMIN:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "superadmin@example.com",
    "password": "sua_senha"
  }'
```

Copie o `accessToken` da resposta.

### 3.2 Criar Configuração do Gateway Stripe

```bash
curl -X POST http://localhost:8080/api/v1/admin/billing/gateways \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "gatewayType": "STRIPE",
    "apiKey": "pk_test_51TIH8ABMB0oMkxTZx6YU0xWj5Ug8mZc0Xzc99TrclcISbhJVb44X3m0WjykYOcWbsrhQk9JmCyrZnmqTZMEyabQL00LjQt0QbH",
    "webhookSecret": "whsec_2b770bb9c33f17af6061c2560ac23748941df2583b7c86ed1eec18b5ea302391",
    "active": true,
    "testMode": true
  }'
```

### 3.3 Verificar Gateway Criado

```bash
curl -X GET http://localhost:8080/api/v1/admin/billing/gateways \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

### 3.4 Ativar Gateway Stripe

```bash
curl -X POST http://localhost:8080/api/v1/admin/billing/gateways/STRIPE/activate \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

---

## 📦 PASSO 4: Criar Planos da Plataforma via API

### 4.1 Criar Plano "Starter"

```bash
curl -X POST http://localhost:8080/api/v1/admin/billing/plans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "name": "STARTER",
    "displayName": "Starter Plan",
    "description": "Perfect for testing and small projects",
    "monthlyPrice": 0.00,
    "currency": "USD",
    "maxApis": 3,
    "maxRequestsPerMonth": 10000,
    "maxTeamMembers": 1,
    "customDomain": false,
    "prioritySupport": false,
    "advancedAnalytics": false,
    "stripePriceId": "price_1234567890abcdef",
    "vinti4PriceId": null,
    "active": true
  }'
```

### 4.2 Criar Plano "Growth"

```bash
curl -X POST http://localhost:8080/api/v1/admin/billing/plans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "name": "GROWTH",
    "displayName": "Growth Plan",
    "description": "For growing businesses and teams",
    "monthlyPrice": 49.00,
    "currency": "USD",
    "maxApis": 10,
    "maxRequestsPerMonth": 100000,
    "maxTeamMembers": 5,
    "customDomain": true,
    "prioritySupport": true,
    "advancedAnalytics": true,
    "stripePriceId": "price_abcdef1234567890",
    "vinti4PriceId": null,
    "active": true
  }'
```

### 4.3 Criar Plano "Business"

```bash
curl -X POST http://localhost:8080/api/v1/admin/billing/plans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "name": "BUSINESS",
    "displayName": "Business Plan",
    "description": "Enterprise-grade features and support",
    "monthlyPrice": 149.00,
    "currency": "USD",
    "maxApis": -1,
    "maxRequestsPerMonth": -1,
    "maxTeamMembers": -1,
    "customDomain": true,
    "prioritySupport": true,
    "advancedAnalytics": true,
    "stripePriceId": "price_fedcba0987654321",
    "vinti4PriceId": null,
    "active": true
  }'
```

**Nota**: `-1` significa "ilimitado"

### 4.4 Listar Todos os Planos

```bash
curl -X GET http://localhost:8080/api/v1/admin/billing/plans \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

### 4.5 Listar Apenas Planos Ativos

```bash
curl -X GET http://localhost:8080/api/v1/admin/billing/plans/active \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

---

## 💰 PASSO 5: Configurar Regras de Taxas de Levantamento

### 5.1 Obter ID do Admin

```bash
# Assumindo que você tem o UUID do usuário SUPERADMIN
ADMIN_ID="seu-uuid-aqui"
```

### 5.2 Criar Regra para Transferência Bancária

```bash
curl -X POST "http://localhost:8080/api/v1/admin/billing/fee-rules?adminId=$ADMIN_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "withdrawalMethod": "BANK_TRANSFER",
    "fixedFee": 2.50,
    "percentageFee": 1.00,
    "minAmount": 10.00,
    "maxAmount": 10000.00,
    "currency": "USD",
    "active": true
  }'
```

### 5.3 Criar Regra para PayPal

```bash
curl -X POST "http://localhost:8080/api/v1/admin/billing/fee-rules?adminId=$ADMIN_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "withdrawalMethod": "PAYPAL",
    "fixedFee": 0.50,
    "percentageFee": 2.50,
    "minAmount": 5.00,
    "maxAmount": 5000.00,
    "currency": "USD",
    "active": true
  }'
```

### 5.4 Criar Regra para Stripe Connect

```bash
curl -X POST "http://localhost:8080/api/v1/admin/billing/fee-rules?adminId=$ADMIN_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "withdrawalMethod": "STRIPE_CONNECT",
    "fixedFee": 0.00,
    "percentageFee": 0.25,
    "minAmount": 1.00,
    "maxAmount": 50000.00,
    "currency": "USD",
    "active": true
  }'
```

### 5.5 Listar Todas as Regras

```bash
curl -X GET http://localhost:8080/api/v1/admin/billing/fee-rules \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

---

## 🧪 PASSO 6: Testar Fluxo Completo de Checkout

### 6.1 Criar Sessão de Checkout

```bash
curl -X POST http://localhost:8080/api/v1/billing/checkout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "planName": "GROWTH",
    "successUrl": "http://localhost:4200/billing/success",
    "cancelUrl": "http://localhost:4200/billing/cancel"
  }'
```

Resposta esperada:
```json
{
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_xxxxx",
  "sessionId": "cs_test_xxxxx"
}
```

### 6.2 Abrir URL de Checkout

1. Copie o `checkoutUrl` da resposta
2. Abra no navegador
3. Use cartão de teste do Stripe:
   - **Número**: `4242 4242 4242 4242`
   - **Data**: Qualquer data futura
   - **CVC**: Qualquer 3 dígitos
   - **CEP**: Qualquer CEP

### 6.3 Verificar Webhook Recebido

No terminal onde o Stripe CLI está rodando, você verá:

```
✔ Received event: checkout.session.completed
✔ Forwarded to http://localhost:8080/api/v1/webhooks/stripe
✔ Response: 200 OK
```

### 6.4 Verificar Dados no Banco

```sql
-- Verificar webhook recebido
SELECT * FROM billing_payment_webhooks ORDER BY created_at DESC LIMIT 1;

-- Verificar carteira do provider criada
SELECT * FROM billing_provider_wallets;

-- Verificar transação de receita
SELECT * FROM billing_wallet_transactions ORDER BY created_at DESC LIMIT 1;

-- Verificar evento de revenue share
SELECT * FROM billing_revenue_share_events ORDER BY created_at DESC LIMIT 1;
```

---

## 📊 PASSO 7: Testar Endpoints de Carteira

### 7.1 Consultar Saldo da Carteira

```bash
curl -X GET http://localhost:8080/api/v1/wallet/balance \
  -H "Authorization: Bearer SEU_TOKEN_PROVIDER"
```

### 7.2 Ver Histórico de Transações

```bash
curl -X GET http://localhost:8080/api/v1/wallet/transactions \
  -H "Authorization: Bearer SEU_TOKEN_PROVIDER"
```

### 7.3 Solicitar Levantamento

```bash
curl -X POST http://localhost:8080/api/v1/withdrawals/request \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_PROVIDER" \
  -d '{
    "amount": 30.00,
    "withdrawalMethod": "PAYPAL",
    "destinationAccount": "provider@example.com"
  }'
```

---

## 🔍 PASSO 8: Monitoramento e Logs

### 8.1 Verificar Logs da Aplicação

```bash
# Ver logs em tempo real
tail -f logs/application.log

# Filtrar logs de billing
grep "billing" logs/application.log
```

### 8.2 Verificar Health do Gateway

```bash
curl -X GET http://localhost:8080/actuator/health
```

### 8.3 Stripe CLI - Ver Eventos

```bash
# Listar eventos recentes
stripe events list --limit 10

# Ver detalhes de um evento específico
stripe events retrieve evt_xxxxx
```

---

## 🚀 PASSO 9: Preparar para Produção

### 9.1 Obter Credenciais de Produção

1. Acesse: https://dashboard.stripe.com/apikeys
2. Ative sua conta Stripe (verificação de identidade)
3. Copie as chaves de **produção** (não test)

### 9.2 Configurar Webhook em Produção

1. Acesse: https://dashboard.stripe.com/webhooks
2. Clique em **"Add endpoint"**
3. URL: `https://seu-dominio.com/api/v1/webhooks/stripe`
4. Selecione eventos:
   - `checkout.session.completed`
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
   - `payment_intent.succeeded`
5. Copie o **Signing secret**

### 9.3 Atualizar Variáveis de Ambiente

```env
# Produção
STRIPE_API_KEY=sk_live_xxxxx  # Chave LIVE
STRIPE_WEBHOOK_SECRET=whsec_xxxxx  # Secret do webhook de produção
STRIPE_PRICE_ID_STARTER=price_live_xxxxx
STRIPE_PRICE_ID_GROWTH=price_live_xxxxx
STRIPE_PRICE_ID_BUSINESS=price_live_xxxxx
```

---

## ✅ Checklist Final

- [ ] Migração V10 executada com sucesso
- [ ] 3 produtos criados no Stripe Dashboard
- [ ] Price IDs atualizados no .env
- [ ] Gateway Stripe configurado via API
- [ ] 3 planos criados (Starter, Growth, Business)
- [ ] Regras de taxas configuradas
- [ ] Checkout testado com cartão de teste
- [ ] Webhook recebido e processado (HTTP 200)
- [ ] Dados salvos corretamente no banco
- [ ] Stripe CLI rodando e funcionando
- [ ] Logs sem erros

---

## 🐛 Troubleshooting

### Erro: "Gateway not configured"

```bash
# Verificar se gateway está ativo
curl -X GET http://localhost:8080/api/v1/admin/billing/gateways/active \
  -H "Authorization: Bearer SEU_TOKEN"
```

### Erro: "Plan not found"

```bash
# Listar planos disponíveis
curl -X GET http://localhost:8080/api/v1/admin/billing/plans/active \
  -H "Authorization: Bearer SEU_TOKEN"
```

### Webhook não está sendo recebido

```bash
# Verificar se Stripe CLI está rodando
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Testar webhook manualmente
stripe trigger checkout.session.completed
```

### Erro de autenticação

```bash
# Verificar se token é válido
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer SEU_TOKEN"
```

---

## 📚 Próximos Passos

1. **Frontend**: Criar interfaces para:
   - Página de administração de planos
   - Página de configuração de gateways
   - Página de gerenciamento de taxas
   - Dashboard de billing para providers

2. **Testes Automatizados**: Criar testes de integração para:
   - Fluxo completo de checkout
   - Processamento de webhooks
   - Cálculo de revenue share
   - Solicitação de levantamentos

3. **Documentação API**: Gerar documentação Swagger/OpenAPI

4. **Monitoramento**: Configurar alertas para:
   - Falhas de webhook
   - Transações suspeitas
   - Erros de gateway

---

## 📞 Suporte

- Documentação Stripe: https://stripe.com/docs
- Stripe CLI: https://stripe.com/docs/stripe-cli
- Dashboard Stripe: https://dashboard.stripe.com
