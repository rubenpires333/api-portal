# Configuração do Stripe Price ID

## Problema Identificado

O plano GROWTH está sendo criado com `amount_due = 0` porque o `stripe_price_id` está NULL no banco de dados.

Quando o Stripe cria uma subscription sem um Price ID válido, ele:
1. Cria a subscription como `active` imediatamente
2. Gera uma invoice com `amount_due = 0`
3. Não cria Payment Intent (porque não há valor a cobrar)

## Logs do Problema

```
18:03:49.529 INFO  - ✅ Subscription created: id=sub_xxx, status=active
18:03:49.536 INFO  - Amount Due: 0
18:03:49.538 INFO  - Amount Paid: 0
18:03:49.540 INFO  - Payment Intent ID: null
18:03:49.540 WARN  - ⚠️ Payment Intent is NULL in invoice
```

## Solução

### 1. Criar Produtos e Preços no Stripe Dashboard

Acesse: https://dashboard.stripe.com/test/products

#### Plano STARTER (Gratuito)
- **Produto**: Starter Plan
- **Preço**: $0.00 USD/mês
- **Price ID**: `price_xxxxxxxxxxxxx` (copiar)

#### Plano GROWTH
- **Produto**: Growth Plan
- **Preço**: $49.00 USD/mês
- **Tipo**: Recurring (mensal)
- **Price ID**: `price_xxxxxxxxxxxxx` (copiar)

#### Plano BUSINESS
- **Produto**: Business Plan
- **Preço**: $199.00 USD/mês
- **Tipo**: Recurring (mensal)
- **Price ID**: `price_xxxxxxxxxxxxx` (copiar)

### 2. Atualizar Banco de Dados

Execute o seguinte SQL para atualizar os planos com os Price IDs do Stripe:

```sql
-- Atualizar STARTER
UPDATE platform_plans 
SET stripe_price_id = 'price_xxxxxxxxxxxxx'  -- Substituir pelo Price ID real
WHERE name = 'STARTER';

-- Atualizar GROWTH
UPDATE platform_plans 
SET stripe_price_id = 'price_1TIauu7KdKMF57NaGkQ8oTah'  -- Substituir pelo Price ID real
WHERE name = 'GROWTH';

-- Atualizar BUSINESS
UPDATE platform_plans 
SET stripe_price_id = 'price_xxxxxxxxxxxxx'  -- Substituir pelo Price ID real
WHERE name = 'BUSINESS';
```

### 3. Verificar Configuração

```sql
SELECT name, monthly_price, currency, stripe_price_id 
FROM platform_plans 
WHERE active = true 
ORDER BY display_order;
```

Resultado esperado:
```
name     | monthly_price | currency | stripe_price_id
---------|---------------|----------|------------------
STARTER  | 0.00          | USD      | price_xxxxx
GROWTH   | 49.00         | USD      | price_xxxxx
BUSINESS | 199.00        | USD      | price_xxxxx
```

## Validação Implementada

O sistema agora valida se o `stripe_price_id` existe antes de criar a subscription:

```java
if (plan.getStripePriceId() == null || plan.getStripePriceId().isEmpty()) {
    throw new IllegalStateException(
        "Plano " + plan.getName() + " não está configurado no Stripe. " +
        "Configure o stripe_price_id no banco de dados antes de usar este plano."
    );
}
```

## Fluxo Correto

### Planos Pagos (GROWTH, BUSINESS)
1. Backend cria Subscription com Price ID válido
2. Stripe cria invoice com `amount_due > 0`
3. Stripe cria Payment Intent para cobrar
4. Frontend exibe formulário de pagamento
5. Usuário preenche dados do cartão
6. Stripe processa pagamento
7. Webhook confirma pagamento
8. Subscription ativada

### Plano Gratuito (STARTER)
1. Backend cria Subscription com Price ID de $0
2. Stripe cria invoice com `amount_due = 0`
3. Subscription ativada imediatamente (sem Payment Intent)
4. Frontend redireciona para sucesso
5. Sem necessidade de webhook

## Notas Importantes

- **Ambiente de Teste**: Use Price IDs do modo Test do Stripe
- **Ambiente de Produção**: Crie novos Price IDs no modo Live
- **Moedas**: Crie Price IDs separados para USD e EUR se necessário
- **Metadata**: O sistema adiciona automaticamente metadata nas subscriptions para rastreamento
