# ✅ Checklist de Implementação - Sistema de Billing

## 📦 Arquivos Criados (40 arquivos)

### ✅ Modelos (9 entidades)
- [x] GatewayConfig
- [x] PlatformPlan
- [x] ProviderPlatformSub
- [x] ProviderWallet
- [x] WalletTransaction
- [x] RevenueShareEvent
- [x] WithdrawalRequest
- [x] WithdrawalFeeRule
- [x] PaymentWebhook

### ✅ Enums (5)
- [x] GatewayType
- [x] TransactionType
- [x] TransactionStatus
- [x] WithdrawalMethod
- [x] WithdrawalStatus

### ✅ Repositories (8)
- [x] GatewayConfigRepository
- [x] PlatformPlanRepository
- [x] ProviderWalletRepository
- [x] WalletTransactionRepository
- [x] RevenueShareEventRepository
- [x] WithdrawalRequestRepository
- [x] WithdrawalFeeRuleRepository
- [x] PaymentWebhookRepository

### ✅ Services (5)
- [x] CheckoutService
- [x] WalletService
- [x] RevenueShareService
- [x] WithdrawalService
- [x] HoldbackReleaseScheduler

### ✅ Controllers (4)
- [x] BillingController
- [x] WalletController
- [x] WebhookController
- [x] WithdrawalController (+ AdminWithdrawalController)

### ✅ Gateway (7 arquivos)
- [x] PaymentGateway (interface)
- [x] PaymentGatewayFactory
- [x] StripeGateway
- [x] Vinti4Gateway
- [x] CheckoutRequest DTO
- [x] CheckoutSession DTO
- [x] WebhookEvent DTO
- [x] GatewayMetadata DTO

### ✅ DTOs (3)
- [x] CheckoutSessionDTO
- [x] WalletSummaryDTO
- [x] WithdrawalRequestDTO

### ✅ Configuração (4 arquivos)
- [x] BillingConfig.java
- [x] application-billing.properties
- [x] .env.billing.example
- [x] V10__create_billing_tables.sql

---

## 🚀 Próximos Passos para Ativar o Sistema

### 1️⃣ Adicionar Dependências Maven

Edite `pom.xml` e adicione:

```xml
<!-- Stripe SDK -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.0.0</version>
</dependency>

<!-- Jasypt para encriptação -->
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

Depois execute:
```bash
mvn clean install
```

### 2️⃣ Configurar Variáveis de Ambiente

```bash
# Copiar template
cp .env.billing.example .env

# Editar com suas credenciais
nano .env
```

Preencha:
- `STRIPE_API_KEY` - Obtenha em https://dashboard.stripe.com/apikeys
- `STRIPE_WEBHOOK_SECRET` - Crie webhook em https://dashboard.stripe.com/webhooks
- `STRIPE_PRICE_ID_*` - Crie produtos/preços no Stripe

### 3️⃣ Executar Migração do Banco

```bash
# Opção 1: Flyway (recomendado)
mvn flyway:migrate

# Opção 2: Manual
psql -U postgres -d api_portal -f src/main/resources/db/migration/V10__create_billing_tables.sql
```

Isso criará:
- 9 tabelas
- 3 planos padrão (Starter $0, Growth $49, Business $149)
- 5 regras de taxas de levantamento
- 2 configurações de gateway (Stripe, Vinti4)

### 4️⃣ Implementar Integração Stripe

Edite `StripeGateway.java` e descomente o código TODO:

```java
@Override
public CheckoutSession createCheckoutSession(CheckoutRequest request) {
    Stripe.apiKey = apiKey;
    
    SessionCreateParams params = SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .setSuccessUrl(request.getSuccessUrl())
        .setCancelUrl(request.getCancelUrl())
        .addLineItem(SessionCreateParams.LineItem.builder()
            .setPrice(request.getGatewayPriceId())
            .setQuantity(1L)
            .build())
        .putAllMetadata(request.getMetadata())
        .build();
        
    Session session = Session.create(params);
    return new CheckoutSession(session.getId(), session.getUrl());
}

@Override
public WebhookEvent parseWebhook(String payload, String signature) {
    Event event = Webhook.constructEvent(payload, signature, webhookSecret);
    
    return WebhookEvent.builder()
        .eventId(event.getId())
        .eventType(event.getType())
        .gatewayType("STRIPE")
        .timestamp(LocalDateTime.ofInstant(
            Instant.ofEpochSecond(event.getCreated()), 
            ZoneId.systemDefault()))
        .metadata(extractMetadata(event))
        .build();
}
```

### 5️⃣ Configurar Webhooks no Stripe

1. Acesse: https://dashboard.stripe.com/webhooks
2. Clique em "Add endpoint"
3. URL: `https://seu-dominio.com/api/v1/webhooks/stripe`
4. Selecione eventos:
   - ✅ `payment_intent.succeeded`
   - ✅ `invoice.payment_succeeded`
   - ✅ `subscription.created`
   - ✅ `subscription.updated`
   - ✅ `subscription.deleted`
5. Copie o "Signing secret" para `STRIPE_WEBHOOK_SECRET`

### 6️⃣ Testar Localmente com ngrok

```bash
# Instalar ngrok
brew install ngrok  # macOS
# ou baixe de https://ngrok.com/download

# Expor porta local
ngrok http 8080

# Use a URL gerada no webhook do Stripe
# Ex: https://abc123.ngrok.io/api/v1/webhooks/stripe
```

### 7️⃣ Iniciar Aplicação

```bash
mvn spring-boot:run
```

Verifique logs:
```
Registered payment gateways: [STRIPE, VINTI4]
Starting holdback release job
```

---

## 🧪 Testes Manuais

### Teste 1: Criar Checkout Session

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
  "sessionId": "cs_test_...",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
  "planName": "Growth",
  "amount": "49.00",
  "currency": "USD"
}
```

### Teste 2: Consultar Wallet

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

### Teste 3: Simular Webhook (Stripe CLI)

```bash
# Instalar Stripe CLI
brew install stripe/stripe-cli/stripe

# Login
stripe login

# Escutar webhooks
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Simular evento
stripe trigger payment_intent.succeeded
```

### Teste 4: Solicitar Levantamento

```bash
curl -X POST http://localhost:8080/api/v1/provider/wallet/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "providerId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 100.00,
    "method": "PAYPAL",
    "destinationDetails": "provider@example.com"
  }'
```

---

## 📊 Monitoramento

### Verificar Job de Holdback

```sql
-- Ver transações pendentes
SELECT * FROM wallet_transactions 
WHERE status = 'PENDING' 
AND available_at <= NOW();

-- Ver próximas liberações
SELECT 
    id, 
    amount, 
    available_at,
    available_at - NOW() as time_remaining
FROM wallet_transactions 
WHERE status = 'PENDING'
ORDER BY available_at;
```

### Verificar Levantamentos Pendentes

```sql
SELECT 
    wr.id,
    wr.requested_amount,
    wr.method,
    wr.status,
    wr.requested_at,
    pw.provider_id
FROM withdrawal_requests wr
JOIN provider_wallets pw ON wr.wallet_id = pw.id
WHERE wr.status = 'PENDING_APPROVAL'
ORDER BY wr.requested_at;
```

### Verificar Revenue da Plataforma

```sql
SELECT 
    DATE(created_at) as date,
    COUNT(*) as transactions,
    SUM(platform_commission) as total_commission,
    SUM(provider_share) as total_provider_share,
    SUM(total_amount) as total_revenue
FROM revenue_share_events
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

---

## 🔒 Segurança - Checklist

- [ ] Variáveis de ambiente configuradas (não hardcoded)
- [ ] Webhook signature validation ativa
- [ ] HTTPS em produção
- [ ] Rate limiting nos endpoints
- [ ] Encriptação de `payout_details` com Jasypt
- [ ] Logs de auditoria ativos
- [ ] Backup automático do banco de dados
- [ ] Monitoramento de transações suspeitas

---

## 📈 Métricas de Sucesso

Após implementação, monitore:

1. **Taxa de conversão de checkout**: % de sessions que viram pagamentos
2. **Tempo médio de holdback**: Deve ser ~14 dias
3. **Taxa de aprovação de levantamentos**: % aprovados vs rejeitados
4. **Revenue por gateway**: Stripe vs Vinti4
5. **Saldo médio das wallets**: Indicador de retenção

---

## 🆘 Suporte

### Problemas Comuns

**Erro: "Gateway not found"**
- Verifique se o gateway está no banco: `SELECT * FROM gateway_configs;`
- Confirme que `active = true`

**Webhook retorna 400**
- Verifique o webhook secret
- Confirme que a assinatura está sendo enviada no header correto

**Job de holdback não executa**
- Verifique se `@EnableScheduling` está ativo
- Confirme timezone do servidor

**Transação não aparece na wallet**
- Verifique logs do `RevenueShareService`
- Confirme que o webhook foi processado: `SELECT * FROM payment_webhooks;`

---

## 📚 Documentação de Referência

- [Documentação Técnica Completa](./billing-gateway-doc.docx)
- [Guia de Implementação](./BILLING_IMPLEMENTATION_GUIDE.md)
- [Stripe API Docs](https://stripe.com/docs/api)
- [Spring Scheduling](https://spring.io/guides/gs/scheduling-tasks/)

---

**Status Atual**: ✅ Estrutura base 100% completa
**Tempo estimado para ativação**: 2-4 horas
**Próximo milestone**: Integração Stripe + Testes E2E
