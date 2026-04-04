# Guia de Implementação - Módulo Billing

## ✅ Arquivos Criados

### Estrutura Completa
```
billing/
├── config/
│   └── BillingConfig.java
├── controller/
│   ├── BillingController.java
│   ├── WalletController.java
│   ├── WebhookController.java
│   └── WithdrawalController.java
├── dto/
│   ├── CheckoutSessionDTO.java
│   ├── WalletSummaryDTO.java
│   └── WithdrawalRequestDTO.java
├── gateway/
│   ├── PaymentGateway.java (interface)
│   ├── PaymentGatewayFactory.java
│   ├── dto/
│   │   ├── CheckoutRequest.java
│   │   ├── CheckoutSession.java
│   │   ├── GatewayMetadata.java
│   │   └── WebhookEvent.java
│   ├── stripe/
│   │   └── StripeGateway.java
│   └── vinti4/
│       └── Vinti4Gateway.java
├── model/
│   ├── GatewayConfig.java
│   ├── PaymentWebhook.java
│   ├── PlatformPlan.java
│   ├── ProviderPlatformSub.java
│   ├── ProviderWallet.java
│   ├── RevenueShareEvent.java
│   ├── WalletTransaction.java
│   ├── WithdrawalFeeRule.java
│   ├── WithdrawalRequest.java
│   └── enums/
│       ├── GatewayType.java
│       ├── TransactionStatus.java
│       ├── TransactionType.java
│       ├── WithdrawalMethod.java
│       └── WithdrawalStatus.java
├── repository/
│   ├── GatewayConfigRepository.java
│   ├── PaymentWebhookRepository.java
│   ├── PlatformPlanRepository.java
│   ├── ProviderWalletRepository.java
│   ├── RevenueShareEventRepository.java
│   ├── WalletTransactionRepository.java
│   ├── WithdrawalFeeRuleRepository.java
│   └── WithdrawalRequestRepository.java
└── service/
    ├── CheckoutService.java
    ├── HoldbackReleaseScheduler.java
    ├── RevenueShareService.java
    ├── WalletService.java
    └── WithdrawalService.java
```

## 📋 Próximos Passos

### 1. Adicionar Dependências ao pom.xml

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

### 2. Configurar Variáveis de Ambiente

Copie `.env.billing.example` para `.env` e preencha com suas credenciais:

```bash
cp .env.billing.example .env
```

### 3. Executar Migração do Banco de Dados

```bash
# Se usar Flyway (recomendado)
mvn flyway:migrate

# Ou execute manualmente o script SQL
psql -U postgres -d api_portal -f src/main/resources/db/migration/V10__create_billing_tables.sql
```

### 4. Implementar Integração Stripe

No arquivo `StripeGateway.java`, descomente e implemente:

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
```

### 5. Configurar Webhooks no Stripe

1. Acesse o Dashboard do Stripe
2. Vá em Developers > Webhooks
3. Adicione endpoint: `https://seu-dominio.com/api/v1/webhooks/stripe`
4. Selecione eventos:
   - `payment_intent.succeeded`
   - `invoice.payment_succeeded`
   - `subscription.created`
   - `subscription.updated`
   - `subscription.deleted`

### 6. Testar o Fluxo Completo

#### Criar Checkout Session
```bash
curl -X POST http://localhost:8080/api/v1/billing/checkout/platform \
  -H "Content-Type: application/json" \
  -d '{
    "providerId": "uuid-do-provider",
    "planName": "GROWTH"
  }'
```

#### Consultar Wallet
```bash
curl http://localhost:8080/api/v1/provider/wallet?providerId=uuid-do-provider
```

#### Solicitar Levantamento
```bash
curl -X POST http://localhost:8080/api/v1/provider/wallet/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "method": "PAYPAL",
    "destinationDetails": "email@example.com"
  }'
```

## 🔐 Segurança

### Encriptação de Dados Sensíveis

Configure Jasypt no `application.properties`:

```properties
jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}
jasypt.encryptor.algorithm=PBEWithMD5AndDES
```

### Validação de Webhooks

Todos os webhooks validam assinatura HMAC antes de processar:

```java
Event event = Webhook.constructEvent(payload, signature, webhookSecret);
```

## 📊 Monitoramento

### Job de Holdback

Executa diariamente às 2h da manhã:
```java
@Scheduled(cron = "0 0 2 * * *")
public void releaseHoldbackTransactions()
```

### Logs Importantes

- Criação de checkout sessions
- Processamento de webhooks
- Liberação de holdback
- Aprovação/rejeição de levantamentos

## 🚀 Deployment

### Variáveis de Ambiente Obrigatórias

```bash
STRIPE_API_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
PLATFORM_COMMISSION_PERCENTAGE=20.00
HOLDBACK_DAYS=14
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

## 📝 Notas Importantes

1. **Holdback de 14 dias**: Protege contra chargebacks
2. **Aprovação automática**: Apenas para valores ≤ $50
3. **Idempotência**: Webhooks duplicados são ignorados
4. **Taxas configuráveis**: Admin pode ajustar via banco de dados
5. **Multi-gateway**: Adicione novos gateways implementando `PaymentGateway`

## 🐛 Troubleshooting

### Webhook não está sendo recebido
- Verifique se o endpoint está acessível publicamente
- Confirme que o webhook secret está correto
- Use ngrok para testes locais: `ngrok http 8080`

### Transações não estão sendo liberadas
- Verifique se o scheduler está ativo (`@EnableScheduling`)
- Confirme que o job está executando nos logs
- Verifique a coluna `available_at` nas transações

### Gateway não encontrado
- Confirme que o gateway está registrado no banco
- Verifique se está marcado como `active = true`
- Reinicie a aplicação para recarregar beans

## 📚 Documentação Adicional

- [Stripe API Docs](https://stripe.com/docs/api)
- [Vinti4 Integration Guide](https://vinti4.com/docs)
- [Spring Scheduling](https://spring.io/guides/gs/scheduling-tasks/)

---

**Status**: ✅ Estrutura base completa
**Próximo**: Implementar integração Stripe e testes
