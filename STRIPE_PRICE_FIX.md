# Correção do Stripe Price ID - GROWTH Plan

## Problema Identificado

O Price ID `price_1TIauu7KdKMF57NaGkQ8oTah` está configurado no Stripe com valor **$0.00** ou em modo trial, mas o plano GROWTH no banco tem valor **€29.99**.

Resultado: Subscription criada sem Payment Intent, ativada imediatamente sem pagamento.

## Logs do Problema

```
18:07:27.366 INFO - Plano encontrado: name=GROWTH, price=29.99 EUR, stripePriceId=price_1TIauu7KdKMF57NaGkQ8oTah
18:07:30.498 INFO - Amount Due: 0
18:07:30.498 INFO - Amount Paid: 0
18:07:30.498 INFO - Payment Intent ID: null
```

## Solução Imediata

### Opção 1: Verificar e Corrigir o Price ID Existente

1. Acesse o Stripe Dashboard: https://dashboard.stripe.com/test/products
2. Procure pelo Price ID: `price_1TIauu7KdKMF57NaGkQ8oTah`
3. Verifique:
   - Valor está correto? (€29.99)
   - Moeda está correta? (EUR)
   - Tipo é Recurring? (Monthly)
   - Não tem trial period configurado?

### Opção 2: Criar Novo Price ID (RECOMENDADO)

Se o Price ID existente estiver incorreto, crie um novo:

1. Acesse: https://dashboard.stripe.com/test/products
2. Clique em "Add product" ou edite o produto "Growth Plan"
3. Configure:
   - **Nome**: Growth Plan
   - **Preço**: €29.99
   - **Moeda**: EUR
   - **Tipo**: Recurring
   - **Intervalo**: Monthly
   - **Trial period**: None (deixe vazio)
4. Copie o novo Price ID gerado (ex: `price_1XYZ...`)
5. Execute o SQL abaixo:

```sql
-- Atualizar GROWTH com novo Price ID
UPDATE platform_plans 
SET 
    stripe_price_id = 'NOVO_PRICE_ID_AQUI',  -- Substituir pelo Price ID correto
    updated_at = NOW()
WHERE name = 'GROWTH';

-- Verificar
SELECT name, monthly_price, currency, stripe_price_id 
FROM platform_plans 
WHERE name = 'GROWTH';
```

## Como Verificar se o Price ID está Correto

### Via Stripe CLI (se instalado)

```bash
stripe prices retrieve price_1TIauu7KdKMF57NaGkQ8oTah
```

Verifique o output:
```json
{
  "id": "price_1TIauu7KdKMF57NaGkQ8oTah",
  "object": "price",
  "active": true,
  "currency": "eur",
  "unit_amount": 2999,  // Deve ser 2999 (€29.99 em centavos)
  "recurring": {
    "interval": "month",
    "trial_period_days": null  // Deve ser null
  }
}
```

### Via Stripe Dashboard

1. Acesse: https://dashboard.stripe.com/test/prices/price_1TIauu7KdKMF57NaGkQ8oTah
2. Verifique:
   - **Amount**: €29.99
   - **Billing period**: Monthly
   - **Trial period**: None

## Teste Após Correção

1. Atualize o Price ID no banco
2. Reinicie a aplicação (se necessário)
3. Tente subscrever ao plano GROWTH novamente
4. Verifique os logs:

```
✅ Esperado:
- Amount Due: 2999 (€29.99 em centavos)
- Payment Intent ID: pi_xxxxx (não null)
- Status: incomplete (aguardando pagamento)

❌ Problema (atual):
- Amount Due: 0
- Payment Intent ID: null
- Status: active (sem pagamento)
```

## Configuração Correta dos 3 Planos

```sql
-- STARTER (Gratuito)
UPDATE platform_plans 
SET stripe_price_id = 'price_STARTER_0EUR'  -- Criar com €0.00
WHERE name = 'STARTER';

-- GROWTH (€29.99/mês)
UPDATE platform_plans 
SET stripe_price_id = 'price_GROWTH_2999EUR'  -- Criar com €29.99
WHERE name = 'GROWTH';

-- BUSINESS (€199.00/mês)
UPDATE platform_plans 
SET stripe_price_id = 'price_BUSINESS_19900EUR'  -- Criar com €199.00
WHERE name = 'BUSINESS';
```

## Notas Importantes

1. **Centavos**: Stripe usa centavos, então €29.99 = 2999
2. **Moeda**: Certifique-se de usar EUR, não USD
3. **Trial**: Não configure trial period se quiser cobrar imediatamente
4. **Test Mode**: Certifique-se de estar no modo Test do Stripe
5. **Metadata**: Adicione metadata ao Price para rastreamento:
   ```
   plan_name: GROWTH
   environment: test
   ```

## Rollback (se necessário)

Se precisar voltar ao comportamento anterior (Payment Intent simples):

```java
// No CheckoutService, comentar a linha:
// Map<String, String> result = stripeGateway.createSubscriptionWithSetupIntent(...)

// E descomentar:
// Map<String, String> result = stripeGateway.createPaymentIntent(...)
```

Mas isso não é recomendado, pois subscriptions são a forma correta de lidar com pagamentos recorrentes.
