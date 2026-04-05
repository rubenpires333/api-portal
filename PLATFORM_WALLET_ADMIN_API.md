# API: Platform Wallet Admin

## 🎯 Objetivo

API para administradores visualizarem:
- 💰 Receitas da plataforma
- 📊 Transações financeiras
- 📋 Subscriptions ativas
- 💳 Levantamentos pendentes/concluídos
- 📈 Estatísticas gerais

## ✅ STATUS: IMPLEMENTADO E FUNCIONAL

Todos os erros de compilação foram corrigidos:
- ✅ Imports corrigidos (removido enum SubscriptionStatus inexistente)
- ✅ Repository atualizado para usar String ao invés de enum
- ✅ DTOs corrigidos para usar String status
- ✅ Mapeamento de entidades corrigido (plan.displayName, plan.monthlyPrice)
- ✅ TransactionType corrigido (DEBIT_PLATFORM_FEE, CREDIT_REVENUE)
- ✅ Aplicação compila e inicia sem erros

---

## 🔐 Autenticação

Todos os endpoints requerem:
- ✅ Token de autenticação (Bearer)
- ✅ Permissão: `billing.manage` (ADMIN ou SUPER_ADMIN)

---

## 📋 Endpoints Disponíveis

### 1. Resumo Financeiro da Plataforma

**GET** `/api/v1/admin/platform-wallet/summary`

Retorna resumo completo das finanças da plataforma.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/summary" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Response:**
```json
{
  "totalSubscriptionRevenue": 490.00,        // Total de subscriptions
  "totalApiCommissionRevenue": 1250.00,      // Total de comissões de API (20%)
  "totalWithdrawalFees": 45.00,              // Total de taxas de levantamento
  "totalRevenue": 1785.00,                   // Total geral
  "activeSubscriptions": 10,                 // Subscriptions ativas
  "monthlyRecurringRevenue": 490.00,         // MRR
  "pendingWithdrawals": 3,                   // Levantamentos pendentes
  "pendingWithdrawalsAmount": 475.00,        // Valor pendente
  "completedWithdrawals": 15,                // Levantamentos concluídos
  "completedWithdrawalsAmount": 3200.00,     // Valor total pago
  "totalProviders": 25,                      // Total de providers
  "activeProviders": 10,                     // Providers com subscription ativa
  "period": "Todos os tempos"
}
```

**Métricas Importantes:**
- **Total Revenue**: Soma de todas as receitas
- **MRR**: Receita mensal recorrente (subscriptions ativas)
- **Active Providers**: Providers pagando subscription

---

### 2. Transações da Plataforma

**GET** `/api/v1/admin/platform-wallet/transactions`

Retorna todas as transações de receita da plataforma (paginado).

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/transactions?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Response:**
```json
{
  "content": [
    {
      "id": "uuid-da-transacao",
      "amount": 49.00,
      "type": "PLATFORM_SUBSCRIPTION_REVENUE",
      "description": "Receita de subscription de plataforma - GROWTH",
      "createdAt": "2026-04-05T14:30:00",
      "provider": {
        "id": "uuid-do-provider",
        "name": "João Silva",
        "email": "joao@email.com",
        "username": "joaosilva"
      },
      "planName": "GROWTH",
      "referenceId": "uuid-da-subscription"
    },
    {
      "id": "uuid-da-transacao-2",
      "amount": 20.00,
      "type": "CREDIT_API_COMMISSION",
      "description": "Comissão de uso de API (20%)",
      "createdAt": "2026-04-05T13:15:00",
      "provider": {
        "id": "uuid-do-provider-2",
        "name": "Maria Santos",
        "email": "maria@email.com",
        "username": "mariasantos"
      },
      "planName": null,
      "referenceId": "uuid-do-payment"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 125,
  "totalPages": 7
}
```

**Tipos de Transação:**
- `PLATFORM_SUBSCRIPTION_REVENUE`: Receita de subscription (100%)
- `CREDIT_API_COMMISSION`: Comissão de API (20%)

---

### 3. Transações por Período

**GET** `/api/v1/admin/platform-wallet/transactions/period`

Retorna transações de um período específico.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/transactions/period?startDate=2026-04-01T00:00:00&endDate=2026-04-30T23:59:59&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Parâmetros:**
- `startDate`: Data inicial (ISO 8601)
- `endDate`: Data final (ISO 8601)
- `page`: Número da página (default: 0)
- `size`: Tamanho da página (default: 20)

**Response:** Mesmo formato do endpoint anterior

**Exemplos de Períodos:**
```bash
# Este mês
startDate=2026-04-01T00:00:00
endDate=2026-04-30T23:59:59

# Últimos 7 dias
startDate=2026-03-29T00:00:00
endDate=2026-04-05T23:59:59

# Último ano
startDate=2025-04-01T00:00:00
endDate=2026-04-01T00:00:00
```

---

### 4. Todas as Subscriptions

**GET** `/api/v1/admin/platform-wallet/subscriptions`

Retorna todas as subscriptions (qualquer status).

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/subscriptions?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Response:**
```json
{
  "content": [
    {
      "id": "uuid-da-subscription",
      "planName": "GROWTH",
      "status": "ACTIVE",
      "amount": 49.00,
      "currency": "EUR",
      "currentPeriodStart": "2026-04-05T00:00:00",
      "currentPeriodEnd": "2026-05-05T00:00:00",
      "createdAt": "2026-04-05T14:30:00",
      "cancelledAt": null,
      "provider": {
        "id": "uuid-do-provider",
        "name": "João Silva",
        "email": "joao@email.com",
        "username": "joaosilva"
      },
      "stripeSubscriptionId": "sub_1ABC123...",
      "stripeCustomerId": "cus_ABC123..."
    },
    {
      "id": "uuid-da-subscription-2",
      "planName": "STARTER",
      "status": "CANCELLED",
      "amount": 19.00,
      "currency": "EUR",
      "currentPeriodStart": "2026-03-01T00:00:00",
      "currentPeriodEnd": "2026-04-01T00:00:00",
      "createdAt": "2026-03-01T10:00:00",
      "cancelledAt": "2026-03-25T15:30:00",
      "provider": {
        "id": "uuid-do-provider-2",
        "name": "Maria Santos",
        "email": "maria@email.com",
        "username": "mariasantos"
      },
      "stripeSubscriptionId": "sub_2DEF456...",
      "stripeCustomerId": "cus_DEF456..."
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 25,
  "totalPages": 2
}
```

**Status Possíveis:**
- `ACTIVE`: Ativa e pagando
- `CANCELLED`: Cancelada
- `PAST_DUE`: Pagamento atrasado
- `UNPAID`: Não paga

---

### 5. Subscriptions Ativas

**GET** `/api/v1/admin/platform-wallet/subscriptions/active`

Retorna apenas subscriptions ativas (status: ACTIVE).

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/subscriptions/active?page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Response:** Mesmo formato do endpoint anterior, mas apenas com status `ACTIVE`

---

## 📊 Exemplos de Uso

### Dashboard Admin - Resumo Financeiro

```typescript
// Angular/TypeScript
async loadPlatformSummary() {
  const summary = await this.http.get<PlatformWalletSummary>(
    '/api/v1/admin/platform-wallet/summary'
  ).toPromise();
  
  console.log('Total Revenue:', summary.totalRevenue);
  console.log('MRR:', summary.monthlyRecurringRevenue);
  console.log('Active Providers:', summary.activeProviders);
}
```

### Listar Transações Recentes

```typescript
async loadRecentTransactions() {
  const transactions = await this.http.get<Page<PlatformTransaction>>(
    '/api/v1/admin/platform-wallet/transactions',
    { params: { page: 0, size: 10, sort: 'createdAt,desc' } }
  ).toPromise();
  
  transactions.content.forEach(t => {
    console.log(`${t.provider.name}: €${t.amount} - ${t.type}`);
  });
}
```

### Relatório Mensal

```typescript
async generateMonthlyReport(year: number, month: number) {
  const startDate = new Date(year, month - 1, 1).toISOString();
  const endDate = new Date(year, month, 0, 23, 59, 59).toISOString();
  
  const transactions = await this.http.get<Page<PlatformTransaction>>(
    '/api/v1/admin/platform-wallet/transactions/period',
    { params: { startDate, endDate, page: 0, size: 1000 } }
  ).toPromise();
  
  const total = transactions.content.reduce((sum, t) => sum + t.amount, 0);
  console.log(`Total do mês ${month}/${year}: €${total}`);
}
```

---

## 🎨 Componentes Frontend Sugeridos

### 1. Dashboard Card - Resumo

```html
<div class="platform-wallet-summary">
  <div class="metric-card">
    <h3>Receita Total</h3>
    <p class="amount">€{{ summary.totalRevenue | number:'1.2-2' }}</p>
  </div>
  
  <div class="metric-card">
    <h3>MRR</h3>
    <p class="amount">€{{ summary.monthlyRecurringRevenue | number:'1.2-2' }}</p>
    <small>{{ summary.activeSubscriptions }} subscriptions ativas</small>
  </div>
  
  <div class="metric-card">
    <h3>Levantamentos Pendentes</h3>
    <p class="amount">€{{ summary.pendingWithdrawalsAmount | number:'1.2-2' }}</p>
    <small>{{ summary.pendingWithdrawals }} aguardando aprovação</small>
  </div>
  
  <div class="metric-card">
    <h3>Providers Ativos</h3>
    <p class="count">{{ summary.activeProviders }} / {{ summary.totalProviders }}</p>
  </div>
</div>
```

### 2. Tabela de Transações

```html
<table class="transactions-table">
  <thead>
    <tr>
      <th>Data</th>
      <th>Provider</th>
      <th>Tipo</th>
      <th>Descrição</th>
      <th>Valor</th>
    </tr>
  </thead>
  <tbody>
    <tr *ngFor="let transaction of transactions">
      <td>{{ transaction.createdAt | date:'short' }}</td>
      <td>{{ transaction.provider.name }}</td>
      <td>
        <span class="badge" [ngClass]="getTypeClass(transaction.type)">
          {{ getTypeLabel(transaction.type) }}
        </span>
      </td>
      <td>{{ transaction.description }}</td>
      <td class="amount">€{{ transaction.amount | number:'1.2-2' }}</td>
    </tr>
  </tbody>
</table>
```

### 3. Lista de Subscriptions

```html
<div class="subscriptions-list">
  <div class="subscription-card" *ngFor="let sub of subscriptions">
    <div class="provider-info">
      <h4>{{ sub.provider.name }}</h4>
      <p>{{ sub.provider.email }}</p>
    </div>
    
    <div class="plan-info">
      <span class="plan-badge">{{ sub.planName }}</span>
      <span class="status-badge" [ngClass]="sub.status">
        {{ sub.status }}
      </span>
    </div>
    
    <div class="amount-info">
      <p class="amount">€{{ sub.amount | number:'1.2-2' }}/mês</p>
      <small>Próxima renovação: {{ sub.currentPeriodEnd | date:'short' }}</small>
    </div>
  </div>
</div>
```

---

## ✅ Checklist de Implementação

- [x] DTOs criados (PlatformWalletSummaryDTO, PlatformTransactionDTO, PlatformSubscriptionDTO)
- [x] Service criado (PlatformWalletService)
- [x] Controller criado (PlatformWalletController)
- [x] Repositories atualizados com métodos necessários
- [x] Endpoints protegidos com `@RequiresPermission("billing.manage")`
- [x] Paginação implementada
- [x] Filtros por período implementados
- [x] Logs adicionados

---

## 🧪 Testar Endpoints

```bash
# 1. Obter token de admin
ADMIN_TOKEN="seu-token-aqui"

# 2. Resumo da plataforma
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/summary" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 3. Transações (primeiras 10)
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/transactions?page=0&size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 4. Subscriptions ativas
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/subscriptions/active?page=0&size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 5. Transações do mês atual
curl -X GET "http://localhost:8080/api/v1/admin/platform-wallet/transactions/period?startDate=2026-04-01T00:00:00&endDate=2026-04-30T23:59:59" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 🎉 Sistema Completo!

Agora o admin pode:
- ✅ Ver receitas totais da plataforma
- ✅ Listar todas as transações
- ✅ Filtrar transações por período
- ✅ Ver subscriptions ativas
- ✅ Monitorar levantamentos pendentes
- ✅ Acompanhar MRR (Monthly Recurring Revenue)
- ✅ Ver estatísticas de providers

**API pronta para integração com frontend!** 🚀
