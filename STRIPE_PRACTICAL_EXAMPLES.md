# 💡 Exemplos Práticos - Gateway Stripe

## Casos de Uso Reais e Código Pronto

---

## 📋 Índice

1. [Fluxo Completo de Pagamento](#1-fluxo-completo-de-pagamento)
2. [Exemplos de Código](#2-exemplos-de-código)
3. [Cenários Comuns](#3-cenários-comuns)
4. [Integrações Frontend](#4-integrações-frontend)
5. [Scripts Úteis](#5-scripts-úteis)

---

## 1. Fluxo Completo de Pagamento

### Cenário: Provider Assina Plano Growth

```
┌─────────────┐
│  1. Provider│  Clica em "Assinar Growth Plan"
│   Frontend  │
└──────┬──────┘
       │ POST /api/v1/billing/checkout/platform
       ↓
┌──────────────┐
│  2. Backend  │  Cria checkout session no Stripe
│CheckoutService│
└──────┬───────┘
       │ StripeGateway.createCheckoutSession()
       ↓
┌──────────────┐
│  3. Stripe   │  Retorna URL de checkout
│     API      │
└──────┬───────┘
       │ checkoutUrl
       ↓
┌──────────────┐
│  4. Provider │  Redireciona para Stripe Checkout
│   Navegador  │  Preenche dados do cartão
└──────┬───────┘
       │ Completa pagamento
       ↓
┌──────────────┐
│  5. Stripe   │  Envia webhook: invoice.payment_succeeded
│   Webhook    │
└──────┬───────┘
       │ POST /api/v1/webhooks/stripe
       ↓
┌──────────────┐
│  6. Backend  │  Valida assinatura
│WebhookController│  Processa evento
└──────┬───────┘
       │
       ↓
┌──────────────┐
│  7. Revenue  │  Calcula: $49 - 20% = $39.20 para provider
│ShareService  │  Cria WalletTransaction (PENDING)
└──────┬───────┘
       │
       ↓
┌──────────────┐
│  8. Wallet   │  Atualiza pendingBalance += $39.20
│   Service    │  lifetimeEarned += $39.20
└──────┬───────┘
       │
       ↓
┌──────────────┐
│  9. Job      │  Após 14 dias: PENDING → AVAILABLE
│  Scheduler   │  availableBalance += $39.20
└──────────────┘
```

---

## 2. Exemplos de Código

### 2.1 Criar Checkout Session (Backend)

```java
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/checkout/platform")
    public ResponseEntity<CheckoutSessionDTO> createCheckout(
            @RequestParam UUID providerId,
            @RequestParam String planName) {
        
        CheckoutSessionDTO session = checkoutService
            .createPlatformSubscriptionCheckout(providerId, planName);
        
        return ResponseEntity.ok(session);
    }
}
```

### 2.2 Processar Webhook (Backend)

```java
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        try {
            // 1. Validar assinatura
            PaymentGateway gateway = gatewayFactory.get("STRIPE");
            WebhookEvent event = gateway.parseWebhook(payload, signature);
            
            // 2. Verificar idempotência
            if (webhookRepo.existsByEventId(event.getEventId())) {
                return ResponseEntity.ok().build();
            }
            
            // 3. Salvar webhook
            PaymentWebhook webhook = PaymentWebhook.builder()
                .eventId(event.getEventId())
                .gatewayType(GatewayType.STRIPE)
                .eventType(event.getEventType())
                .payload(payload)
                .processed(false)
                .build();
            webhookRepo.save(webhook);
            
            // 4. Processar evento
            processWebhookEvent(event, webhook);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    private void processWebhookEvent(WebhookEvent event, PaymentWebhook webhook) {
        switch (event.getEventType()) {
            case "invoice.payment_succeeded":
                handlePaymentSuccess(event);
                break;
            case "customer.subscription.created":
                handleSubscriptionCreated(event);
                break;
            // ... outros eventos
        }
        
        webhook.setProcessed(true);
        webhookRepo.save(webhook);
    }
    
    private void handlePaymentSuccess(WebhookEvent event) {
        UUID subscriptionId = UUID.fromString(event.getMetadata().get("subscriptionId"));
        UUID providerId = UUID.fromString(event.getMetadata().get("providerId"));
        
        revenueShareService.processPayment(
            subscriptionId,
            providerId,
            event.getAmount(),
            event.getCurrency()
        );
    }
}
```

### 2.3 Consultar Wallet (Backend)

```java
@RestController
@RequestMapping("/api/v1/provider/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletSummaryDTO> getWallet(@RequestParam UUID providerId) {
        WalletSummaryDTO summary = walletService.getWalletSummary(providerId);
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransaction>> getTransactions(
            @RequestParam UUID providerId,
            Pageable pageable) {
        
        Page<WalletTransaction> transactions = walletService
            .getTransactionHistory(providerId, pageable);
        
        return ResponseEntity.ok(transactions);
    }
}
```

---

## 3. Cenários Comuns

### 3.1 Provider Solicita Levantamento

```bash
# Request
curl -X POST http://localhost:8080/api/v1/provider/wallet/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "providerId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 100.00,
    "method": "PAYPAL",
    "destinationDetails": "provider@example.com"
  }'

# Response
{
  "id": "wr_xxxxx",
  "requestedAmount": 100.00,
  "feePercentage": 3.00,
  "feeAmount": 3.00,
  "netAmount": 97.00,
  "method": "PAYPAL",
  "status": "APPROVED",  // Auto-aprovado (< $50)
  "requestedAt": "2026-04-04T12:00:00"
}
```

### 3.2 Admin Aprova Levantamento

```bash
# Request
curl -X POST http://localhost:8080/api/v1/admin/withdrawals/wr_xxxxx/approve \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -d "adminId=admin_uuid"

# Response
{
  "message": "Withdrawal approved successfully",
  "withdrawalId": "wr_xxxxx",
  "status": "APPROVED"
}
```

### 3.3 Verificar Saldo Disponível

```bash
# Request
curl http://localhost:8080/api/v1/provider/wallet?providerId=123e4567-e89b-12d3-a456-426614174000

# Response
{
  "availableBalance": 250.00,    // Pode levantar
  "pendingBalance": 150.00,      // Em holdback (14 dias)
  "reservedBalance": 100.00,     // Levantamento em processo
  "lifetimeEarned": 500.00,      // Total histórico
  "currency": "USD",
  "minimumPayout": 10.00
}
```

---

## 4. Integrações Frontend

### 4.1 Angular/React - Criar Checkout

```typescript
// service
async createCheckout(providerId: string, planName: string) {
  const response = await fetch('/api/v1/billing/checkout/platform', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ providerId, planName })
  });
  
  const data = await response.json();
  
  // Redirecionar para Stripe Checkout
  window.location.href = data.checkoutUrl;
}

// component
onSubscribe(planName: string) {
  this.billingService.createCheckout(this.providerId, planName);
}
```

### 4.2 Exibir Wallet

```typescript
interface WalletSummary {
  availableBalance: number;
  pendingBalance: number;
  reservedBalance: number;
  lifetimeEarned: number;
  currency: string;
}

async getWallet(providerId: string): Promise<WalletSummary> {
  const response = await fetch(`/api/v1/provider/wallet?providerId=${providerId}`);
  return response.json();
}
```

```html
<!-- template -->
<div class="wallet-card">
  <h3>Minha Wallet</h3>
  
  <div class="balance">
    <span class="label">Disponível</span>
    <span class="amount">{{ wallet.availableBalance | currency }}</span>
  </div>
  
  <div class="balance pending">
    <span class="label">Pendente (14 dias)</span>
    <span class="amount">{{ wallet.pendingBalance | currency }}</span>
  </div>
  
  <div class="balance reserved">
    <span class="label">Reservado</span>
    <span class="amount">{{ wallet.reservedBalance | currency }}</span>
  </div>
  
  <button (click)="requestWithdrawal()" 
          [disabled]="wallet.availableBalance < wallet.minimumPayout">
    Solicitar Levantamento
  </button>
</div>
```

---

## 5. Scripts Úteis

### 5.1 Testar Todos os Eventos

```bash
#!/bin/bash
# test-webhooks.sh

echo "Testando eventos Stripe..."

events=(
  "checkout.session.completed"
  "customer.subscription.created"
  "customer.subscription.updated"
  "invoice.payment_succeeded"
  "invoice.payment_failed"
)

for event in "${events[@]}"; do
  echo "Testando: $event"
  stripe trigger $event
  sleep 2
done

echo "Testes concluídos!"
```

### 5.2 Monitorar Webhooks em Tempo Real

```bash
#!/bin/bash
# monitor-webhooks.sh

echo "Monitorando webhooks..."

tail -f logs/spring.log | grep --line-buffered "WebhookController" | while read line; do
  echo "[$(date '+%H:%M:%S')] $line"
done
```

### 5.3 Verificar Saúde do Sistema

```bash
#!/bin/bash
# health-check.sh

echo "Verificando saúde do sistema..."

# 1. Aplicação
curl -s http://localhost:8080/actuator/health | jq .

# 2. Gateway Stripe
curl -s http://localhost:8080/api/v1/admin/gateways | jq '.[] | select(.gatewayType=="STRIPE")'

# 3. Banco de dados
psql -U postgres -d api_portal -c "SELECT COUNT(*) FROM provider_wallets;"

# 4. Webhooks pendentes
psql -U postgres -d api_portal -c "SELECT COUNT(*) FROM payment_webhooks WHERE processed = false;"

echo "Verificação concluída!"
```

### 5.4 Liberar Holdback Manualmente

```sql
-- Liberar transações pendentes (use com cuidado!)
UPDATE wallet_transactions
SET status = 'AVAILABLE',
    available_at = NOW()
WHERE status = 'PENDING'
  AND available_at <= NOW();

-- Atualizar saldos das wallets
UPDATE provider_wallets pw
SET available_balance = available_balance + (
    SELECT COALESCE(SUM(amount), 0)
    FROM wallet_transactions wt
    WHERE wt.wallet_id = pw.id
      AND wt.status = 'AVAILABLE'
      AND wt.type = 'CREDIT_REVENUE'
),
pending_balance = (
    SELECT COALESCE(SUM(amount), 0)
    FROM wallet_transactions wt
    WHERE wt.wallet_id = pw.id
      AND wt.status = 'PENDING'
      AND wt.type = 'CREDIT_REVENUE'
);
```

---

## 📊 Queries de Relatórios

### Revenue por Período

```sql
SELECT 
    DATE_TRUNC('month', created_at) as month,
    COUNT(*) as transactions,
    SUM(total_amount) as total_revenue,
    SUM(platform_commission) as platform_revenue,
    SUM(provider_share) as provider_revenue
FROM revenue_share_events
WHERE created_at >= NOW() - INTERVAL '12 months'
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month DESC;
```

### Top Providers por Revenue

```sql
SELECT 
    p.id,
    p.name,
    COUNT(rse.id) as transactions,
    SUM(rse.provider_share) as total_earned,
    pw.available_balance,
    pw.lifetime_earned
FROM providers p
JOIN provider_wallets pw ON pw.provider_id = p.id
LEFT JOIN revenue_share_events rse ON rse.provider_id = p.id
GROUP BY p.id, p.name, pw.available_balance, pw.lifetime_earned
ORDER BY total_earned DESC
LIMIT 10;
```

### Levantamentos por Status

```sql
SELECT 
    status,
    COUNT(*) as count,
    SUM(requested_amount) as total_amount,
    AVG(requested_amount) as avg_amount,
    MIN(requested_at) as oldest_request
FROM withdrawal_requests
GROUP BY status
ORDER BY count DESC;
```

---

## 🎯 Casos de Uso Avançados

### Reembolso Parcial

```java
public void processPartialRefund(UUID subscriptionId, BigDecimal refundAmount) {
    // 1. Buscar evento original
    RevenueShareEvent originalEvent = revenueShareRepo
        .findBySubscriptionId(subscriptionId)
        .orElseThrow();
    
    // 2. Calcular proporção do reembolso
    BigDecimal refundRatio = refundAmount.divide(originalEvent.getTotalAmount(), 4, RoundingMode.HALF_UP);
    BigDecimal providerRefund = originalEvent.getProviderShare().multiply(refundRatio);
    
    // 3. Debitar da wallet
    ProviderWallet wallet = walletService.getOrCreateWallet(originalEvent.getProviderId());
    wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(providerRefund));
    
    // 4. Criar transação de reembolso
    WalletTransaction refundTx = WalletTransaction.builder()
        .wallet(wallet)
        .amount(providerRefund.negate())
        .type(TransactionType.CREDIT_REFUND)
        .status(TransactionStatus.DEBITED)
        .referenceId(subscriptionId)
        .description("Partial refund: " + refundAmount)
        .build();
    
    transactionRepo.save(refundTx);
}
```

### Mudar Plano (Upgrade/Downgrade)

```java
public void changePlan(UUID providerId, String newPlanName) {
    // 1. Buscar subscription atual no Stripe
    ProviderPlatformSub currentSub = subRepo.findByProviderId(providerId);
    
    // 2. Buscar novo plano
    PlatformPlan newPlan = planRepo.findByName(newPlanName).orElseThrow();
    
    // 3. Atualizar no Stripe
    Subscription stripeSubscription = Subscription.retrieve(currentSub.getGatewaySubscriptionId());
    SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
        .addItem(SubscriptionUpdateParams.Item.builder()
            .setId(stripeSubscription.getItems().getData().get(0).getId())
            .setPrice(newPlan.getStripePriceId())
            .build())
        .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
        .build();
    
    stripeSubscription.update(params);
    
    // 4. Atualizar no banco
    currentSub.setPlan(newPlan);
    currentSub.setCurrentPrice(newPlan.getMonthlyPrice());
    subRepo.save(currentSub);
}
```

---

**Criado em:** 04 de Abril de 2026  
**Versão:** 1.0.0  
**Complementa:** STRIPE_GATEWAY_COMPLETE_GUIDE.md
