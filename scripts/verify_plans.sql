-- =====================================================
-- Verificar Planos da Plataforma
-- =====================================================

-- Ver todos os planos
SELECT 
    id,
    name,
    display_name,
    monthly_price,
    currency,
    stripe_price_id,
    active,
    display_order
FROM platform_plans
ORDER BY display_order;

-- Contar planos ativos
SELECT 
    COUNT(*) as total_plans,
    SUM(CASE WHEN active THEN 1 ELSE 0 END) as active_plans,
    SUM(CASE WHEN stripe_price_id IS NOT NULL THEN 1 ELSE 0 END) as with_stripe_id
FROM platform_plans;

-- Ver features dos planos
SELECT 
    name,
    display_name,
    max_apis,
    max_requests_per_month,
    features
FROM platform_plans
ORDER BY display_order;
