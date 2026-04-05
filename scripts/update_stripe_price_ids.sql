-- ============================================
-- Script para Atualizar Stripe Price IDs
-- ============================================
-- 
-- INSTRUÇÕES:
-- 1. Acesse o Stripe Dashboard: https://dashboard.stripe.com/test/products
-- 2. Crie os produtos e preços conforme especificado abaixo
-- 3. Copie os Price IDs gerados pelo Stripe
-- 4. Substitua os valores 'SUBSTITUIR_PELO_PRICE_ID_REAL' pelos IDs reais
-- 5. Execute este script no banco de dados
--
-- ============================================

-- STARTER PLAN (Gratuito - $0.00/mês)
-- Criar no Stripe:
--   - Nome: Starter Plan
--   - Preço: $0.00 USD
--   - Tipo: Recurring (Monthly)
UPDATE platform_plans 
SET 
    stripe_price_id = 'SUBSTITUIR_PELO_PRICE_ID_REAL',  -- Ex: price_1ABC123xyz
    updated_at = NOW()
WHERE name = 'STARTER';

-- GROWTH PLAN ($49.00/mês)
-- Criar no Stripe:
--   - Nome: Growth Plan
--   - Preço: $49.00 USD
--   - Tipo: Recurring (Monthly)
UPDATE platform_plans 
SET 
    stripe_price_id = 'SUBSTITUIR_PELO_PRICE_ID_REAL',  -- Ex: price_1DEF456xyz
    updated_at = NOW()
WHERE name = 'GROWTH';

-- BUSINESS PLAN ($199.00/mês)
-- Criar no Stripe:
--   - Nome: Business Plan
--   - Preço: $199.00 USD
--   - Tipo: Recurring (Monthly)
UPDATE platform_plans 
SET 
    stripe_price_id = 'SUBSTITUIR_PELO_PRICE_ID_REAL',  -- Ex: price_1GHI789xyz
    updated_at = NOW()
WHERE name = 'BUSINESS';

-- ============================================
-- Verificar se os Price IDs foram atualizados
-- ============================================
SELECT 
    name,
    display_name,
    monthly_price,
    currency,
    stripe_price_id,
    CASE 
        WHEN stripe_price_id IS NULL OR stripe_price_id = '' THEN '❌ NÃO CONFIGURADO'
        WHEN stripe_price_id LIKE 'SUBSTITUIR%' THEN '⚠️ PRECISA ATUALIZAR'
        ELSE '✅ CONFIGURADO'
    END as status
FROM platform_plans 
WHERE active = true 
ORDER BY display_order;

-- ============================================
-- NOTAS IMPORTANTES:
-- ============================================
-- 
-- 1. AMBIENTE DE TESTE vs PRODUÇÃO:
--    - Test Mode: Price IDs começam com 'price_' e são para testes
--    - Live Mode: Price IDs diferentes para produção
--    - NÃO misture Price IDs de Test e Live!
--
-- 2. MOEDAS:
--    - Se precisar suportar EUR, crie Price IDs separados
--    - Adicione coluna 'stripe_price_id_eur' se necessário
--
-- 3. METADATA:
--    - Ao criar os produtos no Stripe, adicione metadata:
--      * plan_name: STARTER / GROWTH / BUSINESS
--      * environment: test / production
--
-- 4. WEBHOOKS:
--    - Configure o webhook endpoint no Stripe Dashboard
--    - URL: https://seu-dominio.com/api/v1/webhooks/stripe
--    - Eventos necessários:
--      * customer.subscription.created
--      * customer.subscription.updated
--      * customer.subscription.deleted
--      * invoice.payment_succeeded
--      * invoice.payment_failed
--      * payment_intent.succeeded
--
-- ============================================
