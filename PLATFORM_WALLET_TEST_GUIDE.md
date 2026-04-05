# Guia de Teste: Platform Wallet Admin

## 🎯 Objetivo
Testar o sistema de wallet da plataforma após limpar os dados.

---

## 📋 Pré-requisitos

1. Aplicação rodando
2. Usuário ADMIN autenticado
3. Provider com subscription ativa (para gerar receita)

---

## 🧹 Passo 1: Limpar Dados da Wallet

Execute o script SQL:

```bash
# No MySQL
mysql -u root -p api_portal < scripts/clear_wallet_data.sql
```

Ou execute manualmente no MySQL Workbench/phpMyAdmin:
```sql
-- Ver script: scripts/clear_wallet_data.sql
```

**Resultado esperado:**
- ✅ Todas as transações removidas
- ✅ Todas as subscriptions removidas
- ✅ Todos os levantamentos removidos
- ✅ Saldos das wallets zerados

---

## 💳 Passo 2: Criar Nova Subscription

### 2.1 Provider inicia checkout
```http
POST /api/v1/billing/checkout/platform-subscription
Authorization: Bearer {provider_token}
Content-Type: application/json

{
  "planId": "{plan_id}",
  "paymentMethod": "STRIPE"
}
```

**Resposta esperada:**
```json
{
  "sessionId": "cs_...",
  "checkoutUrl": "https://checkout.stripe.com/...",
  "expiresAt": "2026-04-05T14:30:00"
}
```

### 2.2 Provider completa pagamento no Stripe
- Abrir `checkoutUrl` no navegador
- Usar cartão de teste: `4242 4242 4242 4242`
- Preencher dados e confirmar

### 2.3 Webhook processa pagamento
O webhook `checkout.session.completed` será chamado automaticamente e:
- ✅ Cria `ProviderPlatformSubscription` com status "active"
- ✅ Cria transação `DEBIT_PLATFORM_FEE` na wallet da plataforma
- ✅ Atualiza saldo da wallet do provider

---

## 📊 Passo 3: Verificar Wallet da Plataforma

### 3.1 Ver resumo financeiro
```http
GET /api/v1/admin/platform-wallet/summary
Authorization: Bearer {admin_token}
```

**Resposta esperada:**
```json
{
  "totalSubscriptionRevenue": 29.99,
  "totalApiCommissionRevenue": 0.00,
  "totalWithdrawalFees": 0.00,
  "totalRevenue": 29.99,
  "activeSubscriptions": 1,
  "monthlyRecurringRevenue": 29.99,
  "pendingWithdrawals": 0,
  "pendingWithdrawalsAmount": 0.00,
  "completedWithdrawals": 0,
  "completedWithdrawalsAmount": 0.00,
  "totalProviders": 5,
  "activeProviders": 1,
  "period": "Todos os tempos"
}
```

### 3.2 Ver transações
```http
GET /api/v1/admin/platform-wallet/transactions?page=0&size=10
Authorization: Bearer {admin_token}
```

**Resposta esperada:**
```json
{
  "content": [
    {
      "id": "...",
      "amount": 29.99,
      "type": "DEBIT_PLATFORM_FEE",
      "description": "Subscription fee - Starter Plan",
      "createdAt": "2026-04-05T13:45:00",
      "provider": {
        "id": "...",
        "name": "João Silva",
        "email": "joao@example.com",
        "username": "joao"
      },
      "planName": "Starter",
      "referenceId": "..."
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

### 3.3 Ver subscriptions ativas
```http
GET /api/v1/admin/platform-wallet/subscriptions/active?page=0&size=10
Authorization: Bearer {admin_token}
```

**Resposta esperada:**
```json
{
  "content": [
    {
      "id": "...",
      "planName": "Starter",
      "status": "active",
      "amount": 29.99,
      "currency": "USD",
      "currentPeriodStart": "2026-04-05T13:45:00",
      "currentPeriodEnd": "2026-05-05T13:45:00",
      "createdAt": "2026-04-05T13:45:00",
      "cancelledAt": null,
      "provider": {
        "id": "...",
        "name": "João Silva",
        "email": "joao@example.com",
        "username": "joao"
      },
      "stripeSubscriptionId": "sub_...",
      "stripeCustomerId": "cus_..."
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

## ✅ Validações

### Wallet da Plataforma
- ✅ `totalSubscriptionRevenue` = valor do plano (ex: 29.99)
- ✅ `activeSubscriptions` = 1
- ✅ `monthlyRecurringRevenue` = valor do plano
- ✅ `activeProviders` = 1

### Transações
- ✅ Tipo: `DEBIT_PLATFORM_FEE`
- ✅ Valor: igual ao preço do plano
- ✅ Provider info preenchido
- ✅ Plan name preenchido

### Subscriptions
- ✅ Status: "active"
- ✅ Amount: preço do plano
- ✅ Períodos preenchidos (start/end)
- ✅ Stripe IDs preenchidos

---

## 🔄 Teste Adicional: API Usage Revenue

Para testar receita de comissões de API (20%):

1. Provider faz subscription de uma API
2. Consumer usa a API (gera pagamento)
3. Sistema processa revenue share:
   - 80% para provider
   - 20% para plataforma (CREDIT_REVENUE)

Verificar:
```http
GET /api/v1/admin/platform-wallet/summary
```

Deve mostrar:
- ✅ `totalApiCommissionRevenue` > 0
- ✅ `totalRevenue` = subscriptionRevenue + apiCommissionRevenue

---

## 🐛 Troubleshooting

### Subscription não aparece
- Verificar logs: `logs/application.log`
- Verificar webhook foi chamado
- Verificar status no Stripe Dashboard

### Valores incorretos
- Verificar `TransactionType` usado
- Verificar cálculo no `PlatformSubscriptionService`
- Verificar query no repository

### Erro 403 Forbidden
- Verificar token de admin
- Verificar permissão `billing.manage`

---

## 📝 Notas

- **DEBIT_PLATFORM_FEE**: Receita de subscription (100% plataforma)
- **CREDIT_REVENUE**: Receita de comissão de API (20% plataforma)
- **Status "active"**: String, não enum
- **MRR**: Soma dos valores de todas subscriptions ativas
