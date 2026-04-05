# Captura de Detalhes de Pagamento do Stripe

## Status: ✅ IMPLEMENTADO

## Resumo
Este documento descreve como capturar informações detalhadas dos pagamentos do Stripe, incluindo:
- **Invoice Number** (número da fatura) ✅
- **Payment Method** (tipo de cartão e últimos 4 dígitos) ✅
- **Receipt URL** (link para o recibo) ✅
- **Invoice PDF** (link para o PDF da fatura) ✅

## Implementação Completa

### 1. Modelo de Dados ✅
- `WalletTransaction.java` - Campos adicionados
- `PaymentHistoryDTO.java` - DTO atualizado
- Migração de banco de dados criada: `V21__add_payment_details_to_wallet_transactions.sql`

### 2. Gateway Stripe ✅
- `StripeGateway.getPaymentIntentDetails()` - Método completo que busca:
  - Payment Intent details
  - Payment Method (card brand, last4)
  - Charge details (receipt URL)
  - Invoice details (invoice number, PDF URL)

### 3. Serviços Backend ✅
- `CheckoutWebhookService.processPaymentIntentSucceeded()` - Captura detalhes do pagamento
- `PlatformSubscriptionService.activateSubscription()` - Aceita e passa payment details
- `RevenueShareService.recordPlatformSubscriptionRevenue()` - Salva detalhes na transação

### 4. Fluxo de Dados

```
Webhook (Payment Intent Succeeded)
  ↓
CheckoutWebhookService.processPaymentIntentSucceeded()
  ↓ Busca detalhes do Stripe
StripeGateway.getPaymentIntentDetails(paymentIntentId)
  ↓ Retorna Map<String, Object> com todos os detalhes
PlatformSubscriptionService.activateSubscription(session, event, paymentDetails)
  ↓ Passa detalhes para revenue service
RevenueShareService.recordPlatformSubscriptionRevenue(..., paymentDetails)
  ↓ Salva na transação
WalletTransaction (com todos os campos preenchidos)
```

## Campos Adicionados

### 1. WalletTransaction.java
Adicionados os seguintes campos:

```java
@Column(name = "stripe_payment_intent_id")
private String stripePaymentIntentId; // ID do Payment Intent

@Column(name = "stripe_invoice_id")
private String stripeInvoiceId; // ID da Invoice

@Column(name = "stripe_invoice_number")
private String stripeInvoiceNumber; // Número legível (ex: INV-1234)

@Column(name = "payment_method_type")
private String paymentMethodType; // card, sepa_debit, etc

@Column(name = "card_brand")
private String cardBrand; // visa, mastercard, amex

@Column(name = "card_last4")
private String cardLast4; // Últimos 4 dígitos

@Column(name = "receipt_url")
private String receiptUrl; // URL do recibo

@Column(name = "invoice_pdf_url")
private String invoicePdfUrl; // URL do PDF
```

### 2. PaymentHistoryDTO.java
Adicionados campos correspondentes para o frontend.

### 3. StripeGateway.java
Novo método `getPaymentIntentDetails(String paymentIntentId)` que busca:
- Detalhes do Payment Intent
- Informações do método de pagamento (cartão)
- Dados da invoice
- URLs de recibo e PDF

## Como Usar

### ✅ Implementação Completa

O sistema agora captura automaticamente os detalhes de pagamento quando um webhook `payment_intent.succeeded` é recebido.

**Fluxo Automático:**

1. Webhook recebido → `CheckoutWebhookService.processPaymentIntentSucceeded()`
2. Busca detalhes → `StripeGateway.getPaymentIntentDetails(paymentIntentId)`
3. Ativa subscrição → `PlatformSubscriptionService.activateSubscription(session, event, paymentDetails)`
4. Registra receita → `RevenueShareService.recordPlatformSubscriptionRevenue(..., paymentDetails)`
5. Salva transação → `WalletTransaction` com todos os campos preenchidos

**Tratamento de Erros:**
- Se a busca de detalhes falhar, o sistema continua sem os detalhes (não é crítico)
- Logs de warning são gerados para troubleshooting
- A transação é criada mesmo sem os detalhes opcionais

## Migração de Banco de Dados ✅

**Arquivo:** `V21__add_payment_details_to_wallet_transactions.sql`

```sql
ALTER TABLE wallet_transactions 
ADD COLUMN stripe_payment_intent_id VARCHAR(255),
ADD COLUMN stripe_invoice_id VARCHAR(255),
ADD COLUMN stripe_invoice_number VARCHAR(255),
ADD COLUMN payment_method_type VARCHAR(50),
ADD COLUMN card_brand VARCHAR(50),
ADD COLUMN card_last4 VARCHAR(4),
ADD COLUMN receipt_url TEXT,
ADD COLUMN invoice_pdf_url TEXT;

CREATE INDEX idx_wallet_transactions_payment_intent 
ON wallet_transactions(stripe_payment_intent_id);

CREATE INDEX idx_wallet_transactions_invoice 
ON wallet_transactions(stripe_invoice_id);
```

**Status:** Criado e pronto para aplicar. A migração será executada automaticamente no próximo restart da aplicação.

## Informações Capturadas do Stripe

### Payment Intent
- `id`: ID único do payment intent
- `amount`: Valor em centavos
- `currency`: Moeda (eur, usd, etc)
- `status`: succeeded, failed, etc

### Payment Method (Cartão)
- `type`: "card"
- `brand`: "visa", "mastercard", "amex", etc
- `last4`: Últimos 4 dígitos
- `exp_month`: Mês de expiração
- `exp_year`: Ano de expiração

### Invoice
- `id`: ID da invoice no Stripe
- `number`: Número legível (ex: "INV-1234")
- `invoice_pdf`: URL do PDF
- `hosted_invoice_url`: URL da página da invoice

### Receipt
- `receipt_url`: URL do recibo do Stripe

## Frontend (Próximos Passos)

Os dados já estão sendo capturados e salvos no backend. Para exibir no frontend:

### 1. Atualizar Interface TypeScript

```typescript
interface PaymentHistory {
  // Campos existentes
  id: string;
  amount: number;
  currency: string;
  paidAt: string;
  
  // Novos campos
  invoiceNumber?: string;      // "INV-1234"
  cardBrand?: string;           // "visa"
  cardLast4?: string;           // "4242"
  receiptUrl?: string;          // URL do recibo
  invoicePdfUrl?: string;       // URL do PDF
  paymentMethodType?: string;   // "card"
}
```

### 2. Formatação para Exibição

```typescript
formatPaymentMethod(history: PaymentHistory): string {
  if (history.cardBrand && history.cardLast4) {
    const brand = history.cardBrand.charAt(0).toUpperCase() + 
                  history.cardBrand.slice(1);
    return `${brand} •••• ${history.cardLast4}`;
  }
  return history.paymentMethodType || 'Cartão';
}
```

### 3. Exibir na UI

```html
<div class="payment-details">
  <p><strong>Invoice:</strong> {{ payment.invoiceNumber }}</p>
  <p><strong>Payment Method:</strong> {{ formatPaymentMethod(payment) }}</p>
  <a [href]="payment.receiptUrl" target="_blank">View Receipt</a>
  <a [href]="payment.invoicePdfUrl" target="_blank">Download Invoice PDF</a>
</div>
```

## Benefícios

1. **Rastreabilidade completa** - Todos os pagamentos têm referência ao Stripe
2. **Suporte ao cliente** - Fácil identificar transações específicas
3. **Reconciliação** - Comparar com dados do Stripe Dashboard
4. **UX melhorada** - Mostrar detalhes do cartão usado
5. **Compliance** - Manter histórico completo de transações
