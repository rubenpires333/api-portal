# 🎯 Guia Completo de Integração Stripe

## 📋 Índice
1. [Pré-requisitos](#pré-requisitos)
2. [Configuração Inicial](#configuração-inicial)
3. [Criar Produtos e Preços no Stripe](#criar-produtos-e-preços)
4. [Configurar Webhooks](#configurar-webhooks)
5. [Testar Localmente](#testar-localmente)
6. [Fluxo de Assinatura de Plano](#fluxo-assinatura-plano)
7. [Fluxo de Assinatura de API](#fluxo-assinatura-api)
8. [Troubleshooting](#troubleshooting)

---

## 🔧 Pré-requisitos

### 1. Conta Stripe
- Criar conta em: https://dashboard.stripe.com/register
- Ativar modo de teste
- Obter chaves de API

### 2. Dependências Maven
Verificar se está no `pom.xml`:
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.0.0</version>
</dependency>
```

### 3. Stripe CLI (para testes locais)
```bash
# Windows (via Scoop)
scoop install stripe

# Ou baixar de: https://github.com/stripe/stripe-cli/releases
```

---

## ⚙️ Configuração Inicial

### 1. Obter Chaves da API

1. Acesse: https://dashboard.stripe.com/test/apikeys
2. Copie:
   - **Publishable key** (começa com `pk_test_`)
   - **Secret key** (começa com `sk_test_`)

### 2. Configurar .env

Edite `api-portal-backend/.env`:

```env
# Stripe Configuration
STRIPE_API_KEY=sk_test_SUA_CHAVE_SECRETA_AQUI
STRIPE_WEBHOOK_SECRET=whsec_SERA_GERADO_DEPOIS
STRIPE_PUBLISHABLE_KEY=pk_test_SUA_CHAVE_PUBLICA_AQUI

# Price IDs (serão criados no próximo passo)
STRIPE_PRICE_ID_STARTER=price_XXXXX
STRIPE_PRICE_ID_GROWTH=price_XXXXX
STRIPE_PRICE_ID_BUSINESS=price_XXXXX

# URLs
FRONTEND_URL=http://localhost:4200
```

### 3. Atualizar application.properties

Verificar em `src/main/resources/application.properties`:

```properties
# Billing Stripe
billing.stripe.api-key=${STRIPE_API_KEY}
billing.stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET}
billing.stripe.publishable-key=${STRIPE_PUBLISHABLE_KEY}
```

---

## 💳 Criar Produtos e Preços no Stripe

### Opção 1: Via Dashboard (Recomendado para iniciantes)

#### 1. Criar Produto "Starter Plan"
1. Acesse: https://dashboard.stripe.com/test/products
2. Clique em "Add product"
3. Preencha:
   - **Name**: `Starter Plan`
   - **Description**: `Free plan for testing APIs`
   - **Pricing model**: `Standard pricing`
   - **Price**: `$0.00`
   - **Billing period**: `Monthly`
   - **Currency**: `USD`
4. Clique em "Save product"
5. **Copie o Price ID** (ex: `price_1ABC123...`)
6. Cole no `.env` como `STRIPE_PRICE_ID_STARTER`

#### 2. Criar Produto "Growth Plan"
Repita o processo:
- **Name**: `Growth Plan`
- **Price**: `$49.00`
- **Billing period**: `Monthly`
- Copie o Price ID para `STRIPE_PRICE_ID_GROWTH`

#### 3. Criar Produto "Business Plan"
- **Name**: `Business Plan`
- **Price**: `$149.00`
- **Billing period**: `Monthly`
- Copie o Price ID para `STRIPE_PRICE_ID_BUSINESS`

### Opção 2: Via Stripe CLI (Rápido)

```bash
# Login no Stripe
stripe login

# Criar Starter Plan (Free)
stripe products create \
  --name="Starter Plan" \
  --description="Free plan for testing APIs"

# Criar preço para Starter (copie o product_id do comando anterior)
stripe prices create \
  --product=prod_XXXXX \
  --unit-amount=0 \
  --currency=usd \
  --recurring[interval]=month

# Criar Growth Plan
stripe products create \
  --name="Growth Plan" \
  --description="Professional plan with advanced features"

stripe prices create \
  --product=prod_XXXXX \
  --unit-amount=4900 \
  --currency=usd \
  --recurring[interval]=month

# Criar Business Plan
stripe products create \
  --name="Business Plan" \
  --description="Enterprise plan with unlimited features"

stripe prices create \
  --product=prod_XXXXX \
  --unit-amount=14900 \
  --currency=usd \
  --recurring[interval]=month
```

### 3. Atualizar Banco de Dados

Execute este SQL para atualizar os planos com os Price IDs:

```sql
-- Atualizar Starter Plan
UPDATE platform_plans 
SET stripe_price_id = 'prod_UH9DwZ1UqYvwQz'
WHERE name = 'STARTER';

-- Atualizar Growth Plan
UPDATE platform_plans 
SET stripe_price_id = 'price_XXXXX_GROWTH'
WHERE name = 'GROWTH';

-- Atualizar Business Plan
UPDATE platform_plans 
SET stripe_price_id = 'prod_UH9DwZ1UqYvwQz'
WHERE name = 'BUSINESS';

-- Verificar
SELECT name, display_name, monthly_price, stripe_price_id 
FROM platform_plans;
```

---

## 🔔 Configurar Webhooks

### Para Desenvolvimento Local (Stripe CLI)

#### 1. Iniciar Listener

```bash
# Terminal 1: Iniciar o listener
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Você verá algo como:
# > Ready! Your webhook signing secret is whsec_xxxxx
```

#### 2. Copiar Webhook Secret

Copie o `whsec_xxxxx` e cole no `.env`:
```env
STRIPE_WEBHOOK_SECRET=whsec_xxxxx
```

#### 3. Reiniciar Backend

```bash
# Parar o backend (Ctrl+C)
# Iniciar novamente
mvn spring-boot:run
```

### Para Produção

#### 1. Criar Endpoint no Stripe

1. Acesse: https://dashboard.stripe.com/test/webhooks
2. Clique em "Add endpoint"
3. **Endpoint URL**: `https://seu-dominio.com/api/v1/webhooks/stripe`
4. **Events to send**: Selecione:
   - ✅ `checkout.session.completed`
   - ✅ `invoice.payment_succeeded`
   - ✅ `invoice.paid`
   - ✅ `customer.subscription.created`
   - ✅ `customer.subscription.updated`
   - ✅ `customer.subscription.deleted`
   - ✅ `payment_intent.succeeded`
5. Clique em "Add endpoint"
6. Copie o **Signing secret** para o `.env`

---

## 🧪 Testar Localmente

### 1. Verificar Health Check

```bash
curl http://localhost:8080/api/v1/billing/health
```

Resposta esperada:
```json
{
  "STRIPE": {
    "healthy": true,
    "displayName": "Stripe",
    "supportedCurrencies": ["USD", "EUR", "GBP", "BRL", "CVE"]
  }
}
```

### 2. Criar Checkout Session

```bash
curl -X POST http://localhost:8080/api/v1/billing/checkout/platform \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_JWT" \
  -d '{
    "providerId": "SEU_USER_ID",
    "planName": "GROWTH"
  }'
```

Resposta esperada:
```json
{
  "sessionId": "cs_test_a1b2c3...",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
  "planName": "Growth",
  "amount": "49.00",
  "currency": "USD"
}
```

### 3. Abrir Checkout

Copie o `checkoutUrl` e abra no navegador. Você verá a página de checkout do Stripe.

### 4. Testar Pagamento

Use cartões de teste do Stripe:

| Cartão | Número | Resultado |
|--------|--------|-----------|
| Sucesso | `4242 4242 4242 4242` | Pagamento aprovado |
| Falha | `4000 0000 0000 0002` | Cartão recusado |
| 3D Secure | `4000 0025 0000 3155` | Requer autenticação |

- **Data de validade**: Qualquer data futura (ex: 12/25)
- **CVC**: Qualquer 3 dígitos (ex: 123)
- **CEP**: Qualquer (ex: 12345)

### 5. Verificar Webhook

No terminal onde o `stripe listen` está rodando, você verá:
```
2024-04-04 19:30:00   --> checkout.session.completed [evt_xxx]
2024-04-04 19:30:01   <-- [200] POST http://localhost:8080/api/v1/webhooks/stripe
```

### 6. Verificar no Banco de Dados

```sql
-- Ver webhook recebido
SELECT * FROM payment_webhooks 
ORDER BY created_at DESC 
LIMIT 1;

-- Ver assinatura criada (se implementado)
SELECT * FROM provider_platform_subscriptions 
WHERE provider_id = 'SEU_USER_ID';
```

---

## 📱 Fluxo de Assinatura de Plano

### Frontend → Backend → Stripe → Webhook

```
1. Provider clica em "Upgrade to Growth"
   ↓
2. Frontend chama: POST /api/v1/billing/checkout/platform
   ↓
3. Backend cria checkout session no Stripe
   ↓
4. Frontend redireciona para checkout.stripe.com
   ↓
5. Provider preenche dados do cartão
   ↓
6. Stripe processa pagamento
   ↓
7. Stripe envia webhook: checkout.session.completed
   ↓
8. Backend processa webhook e ativa assinatura
   ↓
9. Provider é redirecionado para /billing/success
```

### Implementação no Frontend

```typescript
// billing.service.ts
createPlatformCheckout(planName: string): Observable<CheckoutSession> {
  return this.http.post<CheckoutSession>(
    `${environment.apiUrl}/billing/checkout/platform`,
    { planName }
  );
}

// plan-selection.component.ts
upgradePlan(planName: string) {
  this.billingService.createPlatformCheckout(planName).subscribe({
    next: (session) => {
      // Redirecionar para Stripe Checkout
      window.location.href = session.checkoutUrl;
    },
    error: (error) => {
      this.toastr.error('Erro ao criar checkout');
    }
  });
}
```

---

## 🔌 Fluxo de Assinatura de API

### Para APIs Pagas

```
1. Consumer encontra API paga
   ↓
2. Consumer clica em "Subscribe"
   ↓
3. Frontend chama: POST /api/v1/billing/checkout/api
   {
     "consumerId": "uuid",
     "apiId": "uuid",
     "planId": "uuid"
   }
   ↓
4. Backend cria checkout session
   ↓
5. Stripe processa pagamento
   ↓
6. Webhook ativa subscription da API
   ↓
7. Consumer recebe API key
```

### Implementação (a ser criada)

```java
// CheckoutService.java
public CheckoutSessionDTO createApiSubscriptionCheckout(
    UUID consumerId, 
    UUID apiId, 
    UUID planId
) {
    // Buscar API e plano
    // Calcular preço
    // Criar checkout session
    // Retornar URL
}
```

---

## 🐛 Troubleshooting

### Erro: "No such price"

**Causa**: Price ID incorreto no banco de dados

**Solução**:
```sql
-- Verificar price IDs
SELECT name, stripe_price_id FROM platform_plans;

-- Atualizar se necessário
UPDATE platform_plans 
SET stripe_price_id = 'price_CORRETO'
WHERE name = 'GROWTH';
```

### Erro: "Invalid API Key"

**Causa**: Chave da API incorreta ou não configurada

**Solução**:
1. Verificar `.env`: `STRIPE_API_KEY=sk_test_...`
2. Reiniciar backend
3. Verificar logs: `Stripe gateway initialized`

### Erro: "Webhook signature verification failed"

**Causa**: Webhook secret incorreto

**Solução**:
1. Parar `stripe listen`
2. Iniciar novamente: `stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe`
3. Copiar novo secret para `.env`
4. Reiniciar backend

### Webhook não é recebido

**Causa**: Stripe CLI não está rodando ou URL incorreta

**Solução**:
```bash
# Verificar se está rodando
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Testar manualmente
stripe trigger checkout.session.completed
```

### Checkout redireciona mas não ativa assinatura

**Causa**: Webhook não está processando corretamente

**Solução**:
1. Verificar logs do backend
2. Verificar tabela `payment_webhooks`
3. Verificar se o evento está sendo processado

---

## ✅ Checklist de Teste

### Configuração
- [ ] Chaves da API configuradas no `.env`
- [ ] Produtos criados no Stripe Dashboard
- [ ] Price IDs atualizados no banco de dados
- [ ] Webhook secret configurado
- [ ] Stripe CLI rodando (desenvolvimento)

### Testes de Checkout
- [ ] Health check retorna `healthy: true`
- [ ] Criar checkout session retorna URL
- [ ] Abrir checkout URL mostra página do Stripe
- [ ] Pagamento com cartão de teste funciona
- [ ] Webhook é recebido após pagamento
- [ ] Dados são salvos no banco de dados

### Testes de Assinatura
- [ ] Assinatura é criada após pagamento
- [ ] Provider tem acesso aos recursos do plano
- [ ] Renovação automática funciona (testar com Stripe CLI)
- [ ] Cancelamento funciona
- [ ] Upgrade/downgrade funciona

---

## 📚 Próximos Passos

1. ✅ Implementar processamento de webhooks completo
2. ✅ Criar fluxo de assinatura de API
3. ✅ Implementar cancelamento de assinatura
4. ✅ Implementar upgrade/downgrade de planos
5. ✅ Adicionar testes automatizados
6. ✅ Criar dashboard de assinaturas
7. ✅ Implementar faturas automáticas

---

## 🔗 Links Úteis

- [Stripe Dashboard](https://dashboard.stripe.com)
- [Stripe API Docs](https://stripe.com/docs/api)
- [Stripe Testing](https://stripe.com/docs/testing)
- [Stripe CLI](https://stripe.com/docs/stripe-cli)
- [Stripe Webhooks](https://stripe.com/docs/webhooks)
- [Stripe Checkout](https://stripe.com/docs/payments/checkout)

---

**Pronto para começar!** 🚀

Siga este guia passo a passo e você terá o Stripe totalmente integrado e funcionando.
