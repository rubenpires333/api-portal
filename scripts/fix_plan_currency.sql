-- =====================================================
-- Corrigir Moeda dos Planos para EUR
-- =====================================================
-- Stripe requer mínimo de 0.50 EUR/USD
-- CVE não é suportado diretamente pelo Stripe
-- =====================================================

-- Atualizar todos os planos para EUR com valores mínimos válidos
UPDATE platform_plans
SET 
    currency = 'EUR',
    monthly_price = CASE 
        WHEN name = 'STARTER' THEN 0.00  -- Free plan
        WHEN name = 'GROWTH' THEN 29.99  -- Mínimo válido para Stripe
        WHEN name = 'BUSINESS' THEN 99.99
        ELSE monthly_price
    END,
    updated_at = NOW()
WHERE currency = 'CVE' OR currency = 'USD';

-- Verificar planos atualizados
SELECT 
    name,
    display_name,
    monthly_price,
    currency,
    max_apis,
    max_requests_per_month,
    active
FROM platform_plans
ORDER BY display_order;

-- Mensagem de sucesso
DO $$
BEGIN
    RAISE NOTICE '=========================================';
    RAISE NOTICE '✅ MOEDA DOS PLANOS ATUALIZADA PARA EUR!';
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'STARTER: 0.00 EUR (Free)';
    RAISE NOTICE 'GROWTH: 29.99 EUR';
    RAISE NOTICE 'BUSINESS: 99.99 EUR';
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'Valores atendem o mínimo do Stripe (0.50 EUR)';
END $$;
