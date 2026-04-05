# Teste: Fluxo Completo de Subscription → Carteira

## 🎯 Objetivo

Testar se ao fazer uma subscription, o sistema:
1. ✅ Cria/atualiza carteira do provider
2. ✅ Registra receita da plataforma (100% do valor)
3. ✅ Cria transação correta
4. ✅ Atualiza saldos corretamente

---

## 📋 Pré-requisitos

- ✅ Sistema rodando
- ✅ Stripe configurado (chaves de teste)
- ✅ Webhook Stripe funcionando
- ✅ Provider criado no sistema

---

## 🧪 Passo a Passo

### 1️⃣ Resetar Carteira (Limpar Dados Antigos)

```bash
psql -U postgres -d api_portal -f scripts/reset_wallet_for_testing.sql
```

**O que faz:**
- Apaga todos os levantamentos
- Apaga todas as transações
- Apaga todas as subscriptions
- Apaga todas as carteiras
- Cria carteira zerada para o provider de teste

**Resultado esperado:**
```
Provider Wallets: 1 registro
Wallet Transactions: 0 registros
Withdrawal Requests: 0 registros
Checkout Sessions: 0 registros
Platform Subscriptions: 0 registros

Carteira criada:
- available_balance: 0.00
- reserved_balance: 0.00
- pending_balance: 0.00
- currency: EUR
```

---

### 2️⃣ Criar Checkout Session (Iniciar Subscription)

```bash
curl -X POST "http://localhost:8080/api/v1/billing/checkout/create-session" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "providerId": "69f2020f-7e2a-42ba-bf32-5821cfebe0c2",
    "planName": "GROWTH",
    "successUrl": "http://localhost:4200/success",
    "cancelUrl": "http://localhost:4200/cancel"
  }'
```

**Resposta esperada:**
```json
{
  "sessionId": "cs_test_...",
  "url": "https://checkout.stripe.com/c/pay/cs_test_...",
  "localSessionId": "uuid-da-sessao-local"
}
```

**O que acontece:**
1. Sistema cria `CheckoutSession` local com status `PENDING`
2. Stripe cria sessão de checkout
3. Retorna URL para pagamento

---

### 3️⃣ Completar Pagamento no Stripe

**Opção A: Via Browser**
1. Abrir URL retornada no passo anterior
2. Usar cartão de teste: `4242 4242 4242 4242`
3. Data: Qualquer data futura (ex: 12/34)
4. CVC: Qualquer 3 dígitos (ex: 123)
5. Clicar em "Pay"

**Opção B: Via Stripe CLI (Simular)**
```bash
stripe trigger checkout.session.completed
```

---

### 4️⃣ Webhook Processa Automaticamente

**Logs esperados:**
```
INFO  Stripe webhook validated: eventId=evt_..., type=checkout.session.completed
INFO  Processing checkout completed: sessionId=cs_test_...
INFO  Checkout session found: localSessionId=...
INFO  Activating subscription: providerId=..., planName=GROWTH
INFO  Subscription activated: subscriptionId=...
INFO  Recording platform subscription revenue: providerId=..., amount=49.00
INFO  Platform subscription revenue recorded: amount=49.00
INFO  Checkout session marked as COMPLETED
```

**O que acontece:**
1. Webhook recebe evento `checkout.session.completed`
2. Sistema busca `CheckoutSession` local
3. Ativa subscription do provider
4. Registra receita da plataforma (100% = 49 EUR)
5. Cria transação na carteira
6. Atualiza saldos

---

### 5️⃣ Verificar Carteira Atualizada

```sql
SELECT 
    provider_id,
    available_balance,
    reserved_balance,
    pending_balance,
    currency,
    updated_at
FROM provider_wallets
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
```

**Resultado esperado:**
```
provider_id: 69f2020f-7e2a-42ba-bf32-5821cfebe0c2
available_balance: 0.00      ← Plataforma fica com 100%
reserved_balance: 0.00
pending_balance: 0.00
currency: EUR
updated_at: 2026-04-05 14:30:00
```

**⚠️ IMPORTANTE:** 
- `available_balance = 0.00` está CORRETO!
- Subscription de plataforma = 100% para plataforma
- Provider NÃO recebe nada na carteira
- Plataforma usa esse dinheiro para pagar custos operacionais

---

### 6️⃣ Verificar Transação Criada

```sql
SELECT 
    id,
    amount,
    type,
    status,
    description,
    created_at,
    available_at
FROM wallet_transactions
WHERE wallet_id = (
    SELECT id FROM provider_wallets 
    WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2'
)
ORDER BY created_at DESC;
```

**Resultado esperado:**
```
id: uuid-da-transacao
amount: 49.00                                    ← Valor do plano GROWTH
type: PLATFORM_SUBSCRIPTION_REVENUE              ← Tipo correto
status: AVAILABLE                                ← Disponível imediatamente
description: Receita de subscription de plataforma - GROWTH
created_at: 2026-04-05 14:30:00
available_at: 2026-04-05 14:30:00               ← Sem holdback
```

**⚠️ NOTA:** 
- Transação é criada mas saldo NÃO aumenta
- Isso é correto: plataforma registra receita mas não credita provider
- Provider paga para usar a plataforma, não recebe

---

### 7️⃣ Verificar Subscription Ativa

```sql
SELECT 
    id,
    provider_id,
    plan_name,
    status,
    amount,
    stripe_subscription_id,
    current_period_start,
    current_period_end,
    created_at
FROM platform_subscriptions
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2'
ORDER BY created_at DESC
LIMIT 1;
```

**Resultado esperado:**
```
id: uuid-da-subscription
provider_id: 69f2020f-7e2a-42ba-bf32-5821cfebe0c2
plan_name: GROWTH
status: ACTIVE                                   ← Ativa
amount: 49.00
stripe_subscription_id: sub_...
current_period_start: 2026-04-05
current_period_end: 2026-05-05                   ← 1 mês depois
created_at: 2026-04-05 14:30:00
```

---

### 8️⃣ Verificar Checkout Session

```sql
SELECT 
    id,
    stripe_session_id,
    status,
    amount,
    metadata,
    created_at,
    completed_at
FROM checkout_sessions
WHERE metadata->>'providerId' = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2'
ORDER BY created_at DESC
LIMIT 1;
```

**Resultado esperado:**
```
id: uuid-da-sessao
stripe_session_id: cs_test_...
status: COMPLETED                                ← Concluída
amount: 49.00
metadata: {"providerId": "...", "planName": "GROWTH", ...}
created_at: 2026-04-05 14:25:00
completed_at: 2026-04-05 14:30:00
```

---

## ✅ Checklist de Validação

- [ ] Carteira resetada com sucesso (saldos zerados)
- [ ] Checkout session criada
- [ ] Pagamento completado no Stripe
- [ ] Webhook recebido e processado
- [ ] Subscription ativada (status: ACTIVE)
- [ ] Transação criada (tipo: PLATFORM_SUBSCRIPTION_REVENUE)
- [ ] Saldo da carteira correto (available: 0.00)
- [ ] Checkout session marcada como COMPLETED
- [ ] Logs sem erros

---

## 🔍 Troubleshooting

### Problema: Carteira não foi atualizada

**Verificar:**
1. Webhook foi recebido?
```bash
grep "checkout.session.completed" logs/application.log
```

2. Subscription foi ativada?
```bash
grep "Activating subscription" logs/application.log
```

3. Receita foi registrada?
```bash
grep "Recording platform subscription revenue" logs/application.log
```

**Solução:**
- Verificar se webhook está configurado corretamente
- Verificar se Stripe CLI está rodando (teste local)
- Verificar logs de erro

### Problema: Transação não foi criada

**Verificar:**
```sql
SELECT COUNT(*) FROM wallet_transactions 
WHERE wallet_id = (SELECT id FROM provider_wallets 
                   WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2');
```

**Se 0:**
- Verificar logs: `grep "Platform subscription revenue recorded" logs/application.log`
- Verificar se `RevenueShareService.recordPlatformSubscriptionRevenue()` foi chamado
- Verificar se há erro de transação no banco

### Problema: Saldo não é 0.00

**Esperado:** `available_balance = 0.00`

**Se diferente:**
- Verificar se há transações antigas
- Resetar carteira novamente
- Verificar lógica em `RevenueShareService`

---

## 📊 Fluxo Completo Resumido

```
1. Provider solicita subscription
    ↓
2. Sistema cria CheckoutSession (PENDING)
    ↓
3. Provider paga no Stripe (49 EUR)
    ↓
4. Webhook: checkout.session.completed
    ↓
5. Sistema ativa subscription (ACTIVE)
    ↓
6. Sistema registra receita plataforma
    ↓
7. Cria transação: PLATFORM_SUBSCRIPTION_REVENUE
    ↓
8. Carteira: available_balance = 0.00 ✅
    (Plataforma fica com 100%)
    ↓
9. CheckoutSession: COMPLETED ✅
```

---

## 🎯 Próximo Teste: API Usage Payment

Após validar subscription, teste pagamento de uso de API:

1. Consumer usa API do provider
2. Sistema cobra 100 EUR
3. 80 EUR vai para provider (carteira)
4. 20 EUR vai para plataforma (comissão)

**Esperado:**
- Provider: `available_balance = 0.00`, `pending_balance = 80.00` (holdback 14 dias)
- Plataforma: Registra 20 EUR de comissão

---

## ✅ Sucesso!

Se todos os passos funcionaram:
- ✅ Sistema cria/atualiza carteira corretamente
- ✅ Registra receita da plataforma (100%)
- ✅ Cria transação correta
- ✅ Saldos estão corretos
- ✅ Subscription ativa

**Sistema de billing funcionando 100%!** 🎉
