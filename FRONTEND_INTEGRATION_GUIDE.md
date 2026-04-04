# 🎨 Frontend Integration Guide - Billing System

## 📋 Overview

Este guia é para desenvolvedores frontend que precisam integrar as interfaces de administração e usuário com o sistema de billing.

---

## 🔐 Autenticação

Todos os endpoints requerem autenticação via Bearer token.

```typescript
// Angular HttpClient example
const headers = new HttpHeaders({
  'Authorization': `Bearer ${this.authService.getToken()}`,
  'Content-Type': 'application/json'
});
```

---

## 🎯 Páginas a Criar

### 1. Admin Panel (SUPERADMIN only)

#### 1.1 Gateway Configuration Page
**Rota sugerida**: `/admin/billing/gateways`

**Funcionalidades**:
- Listar gateways configurados
- Adicionar novo gateway
- Editar gateway existente
- Ativar/desativar gateway
- Ver status de saúde

**Endpoints**:
```typescript
// GET - Listar todos os gateways
GET /api/v1/admin/billing/gateways

// GET - Gateway ativo
GET /api/v1/admin/billing/gateways/active

// POST - Criar gateway
POST /api/v1/admin/billing/gateways
Body: {
  gatewayType: "STRIPE",
  apiKey: string,
  webhookSecret: string,
  active: boolean,
  testMode: boolean
}

// PUT - Atualizar gateway
PUT /api/v1/admin/billing/gateways/{id}

// POST - Ativar gateway
POST /api/v1/admin/billing/gateways/{type}/activate
```

**UI Components**:
- Tabela de gateways
- Formulário de criação/edição
- Toggle para ativar/desativar
- Badge de status (ativo/inativo)

#### 1.2 Plans Management Page
**Rota sugerida**: `/admin/billing/plans`

**Funcionalidades**:
- Listar todos os planos
- Criar novo plano
- Editar plano existente
- Ativar/desativar plano
- Deletar plano

**Endpoints**:
```typescript
// GET - Listar todos os planos
GET /api/v1/admin/billing/plans

// GET - Listar planos ativos
GET /api/v1/admin/billing/plans/active

// GET - Buscar plano por ID
GET /api/v1/admin/billing/plans/{id}

// POST - Criar plano
POST /api/v1/admin/billing/plans
Body: {
  name: string,              // "STARTER", "GROWTH", "BUSINESS"
  displayName: string,       // "Starter Plan"
  description: string,
  monthlyPrice: number,      // 0.00, 49.00, 149.00
  currency: string,          // "USD"
  maxApis: number,           // -1 = unlimited
  maxRequestsPerMonth: number,
  maxTeamMembers: number,
  customDomain: boolean,
  prioritySupport: boolean,
  advancedAnalytics: boolean,
  stripePriceId: string,     // from Stripe Dashboard
  vinti4PriceId: string,     // optional
  active: boolean
}

// PUT - Atualizar plano
PUT /api/v1/admin/billing/plans/{id}

// DELETE - Deletar plano
DELETE /api/v1/admin/billing/plans/{id}

// POST - Toggle status
POST /api/v1/admin/billing/plans/{id}/toggle
```

**UI Components**:
- Cards de planos (grid layout)
- Formulário de criação/edição
- Toggle para features
- Badge de preço
- Botão de ativar/desativar

#### 1.3 Fee Rules Management Page
**Rota sugerida**: `/admin/billing/fee-rules`

**Funcionalidades**:
- Listar regras de taxas
- Criar nova regra
- Editar regra existente
- Ativar/desativar regra

**Endpoints**:
```typescript
// GET - Listar todas as regras
GET /api/v1/admin/billing/fee-rules

// GET - Buscar por método
GET /api/v1/admin/billing/fee-rules/{method}

// POST - Criar regra
POST /api/v1/admin/billing/fee-rules?adminId={adminId}
Body: {
  withdrawalMethod: "BANK_TRANSFER" | "PAYPAL" | "STRIPE_CONNECT",
  fixedFee: number,          // 2.50
  percentageFee: number,     // 1.00 (%)
  minAmount: number,         // 10.00
  maxAmount: number,         // 10000.00
  currency: string,          // "USD"
  active: boolean
}

// PUT - Atualizar regra
PUT /api/v1/admin/billing/fee-rules/{id}?adminId={adminId}

// POST - Toggle status
POST /api/v1/admin/billing/fee-rules/{id}/toggle
```

**UI Components**:
- Tabela de regras
- Formulário de criação/edição
- Input de moeda
- Range de valores (min/max)

---

### 2. Provider Dashboard

#### 2.1 Wallet Page
**Rota sugerida**: `/provider/wallet`

**Funcionalidades**:
- Ver saldo disponível
- Ver saldo retido (holdback)
- Histórico de transações
- Filtrar transações

**Endpoints**:
```typescript
// GET - Saldo da carteira
GET /api/v1/wallet/balance
Response: {
  balance: number,           // Saldo disponível
  heldBalance: number,       // Saldo retido (14 dias)
  currency: string,
  totalEarnings: number
}

// GET - Histórico de transações
GET /api/v1/wallet/transactions
Response: [{
  id: string,
  transactionType: "REVENUE_SHARE" | "WITHDRAWAL" | "REFUND",
  amount: number,
  status: "PENDING" | "COMPLETED" | "FAILED",
  description: string,
  createdAt: string
}]
```

**UI Components**:
- Cards de saldo (disponível vs retido)
- Gráfico de receitas
- Tabela de transações
- Filtros (data, tipo, status)

#### 2.2 Withdrawals Page
**Rota sugerida**: `/provider/withdrawals`

**Funcionalidades**:
- Solicitar levantamento
- Ver solicitações pendentes
- Histórico de levantamentos

**Endpoints**:
```typescript
// POST - Solicitar levantamento
POST /api/v1/withdrawals/request
Body: {
  amount: number,
  withdrawalMethod: "BANK_TRANSFER" | "PAYPAL" | "STRIPE_CONNECT",
  destinationAccount: string  // email, IBAN, etc
}

// GET - Minhas solicitações
GET /api/v1/withdrawals/my-requests
Response: [{
  id: string,
  amount: number,
  withdrawalMethod: string,
  status: "PENDING" | "APPROVED" | "PROCESSING" | "COMPLETED" | "REJECTED",
  destinationAccount: string,
  createdAt: string,
  processedAt: string
}]
```

**UI Components**:
- Formulário de solicitação
- Seletor de método de pagamento
- Calculadora de taxas
- Tabela de solicitações
- Badge de status

---

### 3. Consumer Pages

#### 3.1 Plans Page
**Rota sugerida**: `/plans`

**Funcionalidades**:
- Visualizar planos disponíveis
- Comparar features
- Selecionar plano

**Endpoints**:
```typescript
// GET - Planos ativos (público)
GET /api/v1/admin/billing/plans/active
```

**UI Components**:
- Cards de planos (pricing table)
- Comparação de features
- Botão "Subscribe"
- Badge "Popular" ou "Recommended"

#### 3.2 Checkout Page
**Rota sugerida**: `/checkout/:planName`

**Funcionalidades**:
- Criar sessão de checkout
- Redirecionar para Stripe

**Endpoints**:
```typescript
// POST - Criar checkout
POST /api/v1/billing/checkout
Body: {
  planName: string,          // "STARTER", "GROWTH", "BUSINESS"
  successUrl: string,        // URL de retorno sucesso
  cancelUrl: string          // URL de retorno cancelamento
}
Response: {
  checkoutUrl: string,       // URL do Stripe Checkout
  sessionId: string
}
```

**Fluxo**:
1. Usuário clica em "Subscribe"
2. Frontend chama `/billing/checkout`
3. Backend retorna URL do Stripe
4. Frontend redireciona para Stripe
5. Usuário completa pagamento
6. Stripe redireciona para `successUrl`

---

## 📦 TypeScript Interfaces

```typescript
// Gateway
interface GatewayConfig {
  id: string;
  gatewayType: 'STRIPE' | 'VINTI4';
  apiKey: string;
  webhookSecret: string;
  active: boolean;
  testMode: boolean;
  createdAt: string;
  updatedAt: string;
}

// Plan
interface PlatformPlan {
  id: string;
  name: string;
  displayName: string;
  description: string;
  monthlyPrice: number;
  currency: string;
  maxApis: number;
  maxRequestsPerMonth: number;
  maxTeamMembers: number;
  customDomain: boolean;
  prioritySupport: boolean;
  advancedAnalytics: boolean;
  stripePriceId: string;
  vinti4PriceId?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

// Fee Rule
interface WithdrawalFeeRule {
  id: string;
  withdrawalMethod: 'BANK_TRANSFER' | 'PAYPAL' | 'STRIPE_CONNECT';
  fixedFee: number;
  percentageFee: number;
  minAmount: number;
  maxAmount: number;
  currency: string;
  active: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

// Wallet
interface WalletBalance {
  balance: number;
  heldBalance: number;
  currency: string;
  totalEarnings: number;
}

// Transaction
interface WalletTransaction {
  id: string;
  transactionType: 'REVENUE_SHARE' | 'WITHDRAWAL' | 'REFUND';
  amount: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  description: string;
  createdAt: string;
}

// Withdrawal Request
interface WithdrawalRequest {
  id: string;
  amount: number;
  withdrawalMethod: string;
  status: 'PENDING' | 'APPROVED' | 'PROCESSING' | 'COMPLETED' | 'REJECTED';
  destinationAccount: string;
  createdAt: string;
  processedAt?: string;
}

// Checkout
interface CheckoutSession {
  checkoutUrl: string;
  sessionId: string;
}
```

---

## 🎨 UI/UX Recommendations

### Admin Panel
- Use tabelas com paginação
- Adicione filtros e busca
- Use modais para criação/edição
- Adicione confirmação para ações destrutivas
- Mostre feedback visual (toasts/snackbars)

### Provider Dashboard
- Use cards para métricas principais
- Adicione gráficos de receitas
- Use cores para status (verde=sucesso, amarelo=pendente, vermelho=erro)
- Adicione tooltips explicativos

### Consumer Pages
- Use pricing table responsiva
- Destaque o plano recomendado
- Mostre comparação clara de features
- Use ícones para features
- Adicione FAQ section

---

## 🔒 Permissões

```typescript
// Route Guards
const adminRoutes = [
  '/admin/billing/gateways',
  '/admin/billing/plans',
  '/admin/billing/fee-rules'
];
// Requer: hasRole('SUPERADMIN')

const providerRoutes = [
  '/provider/wallet',
  '/provider/withdrawals'
];
// Requer: hasRole('PROVIDER')

const publicRoutes = [
  '/plans',
  '/checkout/:planName'
];
// Público (mas checkout requer autenticação)
```

---

## 🧪 Testing

### Test Data
```typescript
// Stripe Test Cards
const testCards = {
  success: '4242 4242 4242 4242',
  declined: '4000 0000 0000 0002',
  insufficient: '4000 0000 0000 9995'
};

// Test Plans
const testPlans = [
  { name: 'STARTER', price: 0 },
  { name: 'GROWTH', price: 49 },
  { name: 'BUSINESS', price: 149 }
];
```

---

## 📱 Responsive Design

### Breakpoints Sugeridos
- Mobile: < 768px
- Tablet: 768px - 1024px
- Desktop: > 1024px

### Mobile Considerations
- Stack pricing cards vertically
- Use accordion for plan comparison
- Simplify admin tables (show less columns)
- Use bottom sheets for forms

---

## 🚀 Performance

### Optimization Tips
- Cache planos ativos (raramente mudam)
- Paginate transaction history
- Lazy load admin tables
- Use skeleton loaders
- Debounce search inputs

---

## 📊 Analytics

### Events to Track
```typescript
// Admin
- gateway_created
- gateway_activated
- plan_created
- plan_updated
- fee_rule_created

// Provider
- wallet_viewed
- withdrawal_requested
- transaction_filtered

// Consumer
- plan_viewed
- checkout_started
- checkout_completed
- checkout_cancelled
```

---

## 🐛 Error Handling

```typescript
// Common Errors
interface ApiError {
  status: number;
  message: string;
  code: string;
}

// Error Messages
const errorMessages = {
  401: 'Não autorizado. Faça login novamente.',
  403: 'Sem permissão para esta ação.',
  404: 'Recurso não encontrado.',
  500: 'Erro no servidor. Tente novamente.'
};
```

---

## 📚 Resources

- **Postman Collection**: `Billing_Admin_API.postman_collection.json`
- **API Reference**: `BILLING_QUICK_REFERENCE.md`
- **Architecture**: `BILLING_ARCHITECTURE.md`

---

## ✅ Frontend Checklist

- [ ] Criar serviços Angular para cada módulo
- [ ] Implementar route guards
- [ ] Criar interfaces TypeScript
- [ ] Implementar páginas admin
- [ ] Implementar dashboard provider
- [ ] Implementar páginas consumer
- [ ] Adicionar error handling
- [ ] Adicionar loading states
- [ ] Testar responsividade
- [ ] Adicionar analytics
- [ ] Testar com dados reais

---

Boa sorte com a implementação! 🚀
