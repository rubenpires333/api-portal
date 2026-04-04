# 🚀 Billing System - Quick Reference Card

## 📋 Índice Rápido

- [Setup Inicial](#setup-inicial)
- [Endpoints Admin](#endpoints-admin)
- [Testes Rápidos](#testes-rápidos)
- [Comandos SQL Úteis](#comandos-sql-úteis)
- [Troubleshooting](#troubleshooting)

---

## 🔧 Setup Inicial

### 1. Executar Migração
```bash
# A migração roda automaticamente ao iniciar
mvn spring-boot:run -DskipTests
```

### 2. Criar Produtos no Stripe
1. Acesse: https://dashboard.stripe.com/test/products
2. Crie 3 produtos (Starter $0, Growth $49, Business $149)
3. Copie os Price IDs

### 3. Atualizar .env
```env
STRIPE_PRICE_ID_STARTER=price_xxxxx
STRIPE_PRICE_ID_GROWTH=price_xxxxx
STRIPE_PRICE_ID_BUSINESS=price_xxxxx
```

### 4. Rodar Script de Setup
```bash
# Linux/Mac
bash scripts/setup-billing.sh

# Windows
powershell -ExecutionPolicy Bypass -File scripts/setup-billing.ps1
```

---

## 🔐 Endpoints Admin

### Autenticação
```bash
# Login
POST /api/v1/auth/login
{
  "email": "superadmin@example.com",
  "password": "senha"
}
# Resposta: { "accessToken": "...", "userId": "..." }
```

### Gateway Management

```bash
# Criar Gateway
POST /api/v1/admin/billing/gateways
Authorization: Bearer {token}
{
  "gatewayType": "STRIPE",
  "apiKey": "pk_test_...",
  "webhookSecret": "whsec_...",
  "active": true,
  "testMode": true
}

# Listar Gateways
GET /api/v1/admin/billing/gateways
Authorization: Bearer {token}

# Ativar Gateway
POST /api/v1/admin/billing/gateways/STRIPE/activate
Authorization: Bearer {token}
```

### Plan Management

```bash
# Criar Plano
POST /api/v1/admin/billing/plans
Authorization: Bearer {token}
{
  "name": "GROWTH",
  "displayName": "Growth Plan",
  "description": "For growing businesses",
  "monthlyPrice": 49.00,
  "currency": "USD",
  "maxApis": 10,
  "maxRequestsPerMonth": 100000,
  "maxTeamMembers": 5,
  "customDomain": true,
  "prioritySupport": true,
  "advancedAnalytics": true,
  "stripePriceId": "price_xxxxx",
  "active": true
}

# Listar Planos
GET /api/v1/admin/billing/plans
Authorization: Bearer {token}

# Listar Planos Ativos
GET /api/v1/admin/billing/plans/active
Authorization: Bearer {token}

# Toggle Status do Plano
POST /api/v1/admin/billing/plans/{planId}/toggle
Authorization: Bearer {token}
```

### Fee Rule Management

```bash
# Criar Regra de Taxa
POST /api/v1/admin/billing/fee-rules?adminId={adminId}
Authorization: Bearer {token}
{
  "withdrawalMethod": "PAYPAL",
  "fixedFee": 0.50,
  "percentageFee": 2.50,
  "minAmount": 5.00,
  "maxAmount": 5000.00,
  "currency": "USD",
  "active": true
}

# Listar Regras
GET /api/v1/admin/billing/fee-rules
Authorization: Bearer {token}

# Toggle Status da Regra
POST /api/v1/admin/billing/fee-rules/{ruleId}/toggle
Authorization: Bearer {token}
```

---

## 🧪 Testes Rápidos

### Teste 1: Criar Checkout Session
```bash
curl -X POST http://localhost:8080/api/v1/billing/checkout \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "planName": "GROWTH",
    "successUrl": "http://localhost:4200/success",
    "cancelUrl": "http://localhost:4200/cancel"
  }'
```

### Teste 2: Simular Pagamento
1. Abra a URL retornada no checkout
2. Use cartão de teste: `4242 4242 4242 4242`
3. Data: qualquer futura, CVC: qualquer 3 dígitos

### Teste 3: Verificar Webhook
```bash
# Terminal 1: Stripe CLI
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Terminal 2: Trigger evento
stripe trigger checkout.session.completed
```

### Teste 4: Consultar Carteira
```bash
curl -X GET http://localhost:8080/api/v1/wallet/balance \
  -H "Authorization: Bearer {provider_token}"
```

### Teste 5: Solicitar Levantamento
```bash
curl -X POST http://localhost:8080/api/v1/withdrawals/request \
  -H "Authorization: Bearer {provider_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 30.00,
    "withdrawalMethod": "PAYPAL",
    "destinationAccount": "provider@example.com"
  }'
```

---

## 💾 Comandos SQL Úteis

### Verificar Tabelas Criadas
```sql
\dt billing_*
```

### Ver Gateways Configurados
```sql
SELECT id, gateway_type, active, test_mode 
FROM billing_gateway_configs;
```

### Ver Planos Criados
```sql
SELECT id, name, display_name, monthly_price, active 
FROM billing_platform_plans;
```

### Ver Carteiras dos Providers
```sql
SELECT pw.id, pw.provider_id, pw.balance, pw.held_balance, pw.currency
FROM billing_provider_wallets pw;
```

### Ver Transações Recentes
```sql
SELECT wt.id, wt.transaction_type, wt.amount, wt.status, wt.created_at
FROM billing_wallet_transactions wt
ORDER BY wt.created_at DESC
LIMIT 10;
```

### Ver Webhooks Recebidos
```sql
SELECT id, event_type, gateway_type, processed, created_at
FROM billing_payment_webhooks
ORDER BY created_at DESC
LIMIT 10;
```

### Ver Solicitações de Levantamento
```sql
SELECT wr.id, wr.amount, wr.withdrawal_method, wr.status, wr.created_at
FROM billing_withdrawal_requests wr
ORDER BY wr.created_at DESC;
```

### Ver Eventos de Revenue Share
```sql
SELECT rse.id, rse.subscription_id, rse.gross_amount, 
       rse.platform_commission, rse.provider_amount, rse.created_at
FROM billing_revenue_share_events rse
ORDER BY rse.created_at DESC
LIMIT 10;
```

---

## 🐛 Troubleshooting

### Problema: "Gateway not configured"
```bash
# Verificar gateway ativo
curl -X GET http://localhost:8080/api/v1/admin/billing/gateways/active \
  -H "Authorization: Bearer {token}"

# Se não houver, criar e ativar
# Ver seção "Gateway Management" acima
```

### Problema: "Plan not found"
```bash
# Listar planos disponíveis
curl -X GET http://localhost:8080/api/v1/admin/billing/plans/active \
  -H "Authorization: Bearer {token}"
```

### Problema: Webhook não recebido
```bash
# 1. Verificar Stripe CLI está rodando
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# 2. Verificar webhook secret no .env
cat .env | grep STRIPE_WEBHOOK_SECRET

# 3. Testar manualmente
stripe trigger checkout.session.completed
```

### Problema: Erro 401 Unauthorized
```bash
# Verificar token válido
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer {token}"

# Se inválido, fazer login novamente
```

### Problema: Erro ao criar plano
```bash
# Verificar se Price ID existe no Stripe
stripe prices retrieve price_xxxxx

# Verificar se gateway está configurado
curl -X GET http://localhost:8080/api/v1/admin/billing/gateways \
  -H "Authorization: Bearer {token}"
```

---

## 📊 Monitoramento

### Logs da Aplicação
```bash
# Ver logs em tempo real
tail -f logs/application.log

# Filtrar logs de billing
grep "billing" logs/application.log

# Ver erros
grep "ERROR" logs/application.log | grep "billing"
```

### Stripe Dashboard
- Events: https://dashboard.stripe.com/test/events
- Payments: https://dashboard.stripe.com/test/payments
- Customers: https://dashboard.stripe.com/test/customers
- Webhooks: https://dashboard.stripe.com/test/webhooks

### Stripe CLI
```bash
# Listar eventos recentes
stripe events list --limit 10

# Ver detalhes de evento
stripe events retrieve evt_xxxxx

# Listar customers
stripe customers list --limit 10

# Ver detalhes de pagamento
stripe payment_intents retrieve pi_xxxxx
```

---

## 🔑 Cartões de Teste Stripe

| Cenário | Número do Cartão | Resultado |
|---------|------------------|-----------|
| Sucesso | 4242 4242 4242 4242 | Pagamento aprovado |
| Falha | 4000 0000 0000 0002 | Cartão recusado |
| 3D Secure | 4000 0027 6000 3184 | Requer autenticação |
| Insuficiente | 4000 0000 0000 9995 | Fundos insuficientes |

**Nota**: Use qualquer data futura e qualquer CVC de 3 dígitos

---

## 📚 Documentação Completa

- **Setup Detalhado**: `ADMIN_BILLING_SETUP_GUIDE.md`
- **Guia Stripe**: `STRIPE_GATEWAY_COMPLETE_GUIDE.md`
- **Exemplos Práticos**: `STRIPE_PRACTICAL_EXAMPLES.md`
- **Postman Collection**: `Billing_Admin_API.postman_collection.json`

---

## 🎯 Checklist Rápido

- [ ] Migração V10 executada
- [ ] 3 produtos criados no Stripe
- [ ] Price IDs no .env
- [ ] Gateway configurado
- [ ] 3 planos criados
- [ ] Regras de taxas criadas
- [ ] Stripe CLI rodando
- [ ] Checkout testado
- [ ] Webhook recebido (HTTP 200)
- [ ] Dados no banco verificados

---

## 💡 Dicas

1. **Use Postman**: Importe a collection para facilitar os testes
2. **Stripe CLI**: Mantenha rodando durante desenvolvimento
3. **Logs**: Monitore os logs para ver o processamento em tempo real
4. **Test Mode**: Sempre use test mode durante desenvolvimento
5. **Backup**: Faça backup do banco antes de testes destrutivos

---

## 🆘 Suporte

- **Stripe Docs**: https://stripe.com/docs
- **Stripe CLI**: https://stripe.com/docs/stripe-cli
- **Dashboard**: https://dashboard.stripe.com
- **Status**: https://status.stripe.com
