-- ============================================
-- SETUP PARA PAGAMENTO EMBUTIDO
-- ============================================
-- Execute este script DEPOIS de criar os produtos no Stripe Dashboard

-- 1. Limpar Price IDs inválidos
UPDATE platform_plans 
SET stripe_price_id = NULL, 
    updated_at = NOW();

-- 2. Atualizar descrições dos planos
UPDATE platform_plans 
SET description = 'Perfeito para começar e testar suas APIs. Inclui recursos básicos para desenvolvimento.'
WHERE name = 'STARTER';

UPDATE platform_plans 
SET description = 'Para desenvolvedores profissionais e pequenas equipes. Recursos avançados e suporte prioritário.'
WHERE name = 'GROWTH';

UPDATE platform_plans 
SET description = 'Para empresas com necessidades avançadas. Recursos ilimitados e suporte dedicado 24/7.'
WHERE name = 'BUSINESS';

-- 3. Verificar planos
SELECT 
    id,
    name,
    display_name,
    monthly_price,
    stripe_price_id,
    active
FROM platform_plans
ORDER BY display_order;

-- ============================================
-- PRÓXIMOS PASSOS:
-- ============================================
-- 1. Acesse: https://dashboard.stripe.com/test/products
-- 2. Crie 2 produtos (Growth e Business) com preços recorrentes mensais
-- 3. Copie os Price IDs (começam com price_)
-- 4. Execute os comandos abaixo com os IDs reais:

-- UPDATE platform_plans 
-- SET stripe_price_id = 'price_SEU_ID_GROWTH_AQUI' 
-- WHERE name = 'GROWTH';

-- UPDATE platform_plans 
-- SET stripe_price_id = 'price_SEU_ID_BUSINESS_AQUI' 
-- WHERE name = 'BUSINESS';
