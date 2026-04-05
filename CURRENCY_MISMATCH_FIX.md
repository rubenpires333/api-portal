# PROBLEMA CRÍTICO: Incompatibilidade de Moeda

## Problema Identificado

O Price ID `price_1TIauu7KdKMF57NaGkQ8oTah` está configurado com:
- **Moeda**: CVE (Cabo Verde Escudo)
- **Valor**: 49.00 CVE (4900 centavos)

Mas o plano no banco de dados está configurado com:
- **Moeda**: EUR (Euro)
- **Valor**: 29.99 EUR

## Por que isso causa `amount_due = 0`?

O Stripe não suporta CVE (Cabo Verde Escudo) para subscriptions recorrentes. Quando tenta criar uma subscription com uma moeda não suportada, o Stripe:

1. Cria a subscription como `active`
2. Gera invoice com `amount_due = 0`
3. Não cria Payment Intent
4. Marca como paga automaticamente

## Logs do Problema

```
18:13:37.638 INFO - Currency: cve  ❌ ERRADO!
18:13:37.638 INFO - Unit Amount: 4900 (in cents)
18:13:40.001 INFO - Amount Due: 0
18:13:40.001 INFO - Payment Intent ID: null
```

## Solução URGENTE

### 1. Criar Novo Price ID com EUR

Acesse o Stripe Dashboard e crie um novo Price:

**URL**: https://dashboard.stripe.com/test/products

**Configuração**:
- Produto: Growth Plan (ou crie novo)
- Preço: **€29.99**
- Moeda: **EUR** (não CVE!)
- Tipo: Recurring
- Intervalo: Monthly
- Trial: None

### 2. Copiar o Novo Price ID

Após criar, copie o Price ID gerado (ex: `price_1XYZ...`)

### 3. Atualizar Banco de Dados

```sql
-- Atualizar GROWTH com o novo Price ID (EUR)
UPDATE platform_plans 
SET 
    stripe_price_id = 'NOVO_PRICE_ID_EUR',  -- Substituir pelo Price ID correto
    updated_at = NOW()
WHERE name = 'GROWTH';

-- Verificar
SELECT name, monthly_price, currency, stripe_price_id 
FROM platform_plans 
WHERE name = 'GROWTH';
```

### 4. Testar Novamente

Após atualizar, teste a subscrição. Você deve ver:

```
✅ Esperado:
- Currency: eur
- Unit Amount: 2999 (€29.99 em centavos)
- Amount Due: 2999
- Payment Intent ID: pi_xxxxx (não null)
- Status: incomplete (aguardando pagamento)
```

## Moedas Suportadas pelo Stripe

O Stripe suporta as seguintes moedas para subscriptions:

✅ **Suportadas**:
- EUR (Euro)
- USD (Dólar Americano)
- GBP (Libra Esterlina)
- BRL (Real Brasileiro)
- CAD (Dólar Canadense)
- E muitas outras...

❌ **NÃO Suportadas**:
- CVE (Cabo Verde Escudo)
- Moedas de países com restrições

Lista completa: https://stripe.com/docs/currencies

## Configuração Correta dos 3 Planos

Todos os planos devem usar EUR (ou USD):

```sql
-- STARTER (Gratuito)
-- Criar Price no Stripe: €0.00 EUR
UPDATE platform_plans 
SET stripe_price_id = 'price_STARTER_EUR'
WHERE name = 'STARTER';

-- GROWTH (€29.99/mês)
-- Criar Price no Stripe: €29.99 EUR
UPDATE platform_plans 
SET stripe_price_id = 'price_GROWTH_EUR'
WHERE name = 'GROWTH';

-- BUSINESS (€199.00/mês)
-- Criar Price no Stripe: €199.00 EUR
UPDATE platform_plans 
SET stripe_price_id = 'price_BUSINESS_EUR'
WHERE name = 'BUSINESS';
```

## Nota sobre Mudança de Plano

Você mencionou estar mudando de BUSINESS para GROWTH. Isso é um **downgrade**.

Para downgrades, o sistema deve:
1. Cancelar a subscription atual (BUSINESS)
2. Criar nova subscription (GROWTH)
3. Processar pagamento proporcional (proration)

Certifique-se de que o fluxo de downgrade está implementado corretamente.

## Próximos Passos

1. ✅ Criar novo Price ID no Stripe com EUR
2. ✅ Atualizar banco de dados
3. ✅ Testar subscrição novamente
4. ✅ Verificar logs para confirmar `currency: eur`
5. ✅ Confirmar que Payment Intent é criado

## Comando Stripe CLI (Opcional)

Se tiver Stripe CLI instalado:

```bash
# Criar novo Price com EUR
stripe prices create \
  --product prod_XXXXXXXX \
  --currency eur \
  --unit-amount 2999 \
  --recurring[interval]=month \
  --metadata[plan_name]=GROWTH \
  --metadata[environment]=test

# Verificar Price criado
stripe prices retrieve price_NOVO_ID
```
