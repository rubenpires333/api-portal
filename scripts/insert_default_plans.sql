-- =====================================================
-- Inserir Planos Padrão da Plataforma
-- =====================================================

-- Limpar planos existentes (opcional - comente se não quiser limpar)
-- DELETE FROM platform_plans;

-- Inserir Starter Plan (Free)
INSERT INTO platform_plans (
    id,
    name,
    display_name,
    description,
    monthly_price,
    currency,
    max_apis,
    max_requests_per_month,
    custom_domain,
    priority_support,
    advanced_analytics,
    active,
    display_order,
    stripe_price_id,
    vinti4_price_id,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'STARTER',
    'Starter',
    'Perfeito para começar e testar suas APIs',
    0.00,
    'USD',
    3,
    1000,
    false,
    false,
    false,
    true,
    1,
    NULL, -- Será preenchido depois
    NULL,
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    monthly_price = EXCLUDED.monthly_price,
    max_apis = EXCLUDED.max_apis,
    max_requests_per_month = EXCLUDED.max_requests_per_month,
    custom_domain = EXCLUDED.custom_domain,
    priority_support = EXCLUDED.priority_support,
    advanced_analytics = EXCLUDED.advanced_analytics,
    active = EXCLUDED.active,
    display_order = EXCLUDED.display_order,
    updated_at = NOW();

-- Inserir Growth Plan
INSERT INTO platform_plans (
    id,
    name,
    display_name,
    description,
    monthly_price,
    currency,
    max_apis,
    max_requests_per_month,
    custom_domain,
    priority_support,
    advanced_analytics,
    active,
    display_order,
    stripe_price_id,
    vinti4_price_id,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'GROWTH',
    'Growth',
    'Para desenvolvedores profissionais e pequenas equipes',
    49.00,
    'USD',
    10,
    50000,
    false,
    true,
    true,
    true,
    2,
    NULL, -- Será preenchido depois
    NULL,
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    monthly_price = EXCLUDED.monthly_price,
    max_apis = EXCLUDED.max_apis,
    max_requests_per_month = EXCLUDED.max_requests_per_month,
    custom_domain = EXCLUDED.custom_domain,
    priority_support = EXCLUDED.priority_support,
    advanced_analytics = EXCLUDED.advanced_analytics,
    active = EXCLUDED.active,
    display_order = EXCLUDED.display_order,
    updated_at = NOW();

-- Inserir Business Plan
INSERT INTO platform_plans (
    id,
    name,
    display_name,
    description,
    monthly_price,
    currency,
    max_apis,
    max_requests_per_month,
    custom_domain,
    priority_support,
    advanced_analytics,
    active,
    display_order,
    stripe_price_id,
    vinti4_price_id,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'BUSINESS',
    'Business',
    'Para empresas com necessidades avançadas',
    149.00,
    'USD',
    -1, -- Ilimitado
    500000,
    true,
    true,
    true,
    true,
    3,
    NULL, -- Será preenchido depois
    NULL,
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    monthly_price = EXCLUDED.monthly_price,
    max_apis = EXCLUDED.max_apis,
    max_requests_per_month = EXCLUDED.max_requests_per_month,
    custom_domain = EXCLUDED.custom_domain,
    priority_support = EXCLUDED.priority_support,
    advanced_analytics = EXCLUDED.advanced_analytics,
    active = EXCLUDED.active,
    display_order = EXCLUDED.display_order,
    updated_at = NOW();

-- Verificar planos inseridos
SELECT 
    name,
    display_name,
    monthly_price,
    max_apis,
    max_requests_per_month,
    active,
    display_order
FROM platform_plans
ORDER BY display_order;

-- Mensagem de sucesso
DO $$
BEGIN
    RAISE NOTICE '=========================================';
    RAISE NOTICE '✅ PLANOS PADRÃO INSERIDOS COM SUCESSO!';
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'Total de planos: %', (SELECT COUNT(*) FROM platform_plans);
    RAISE NOTICE 'Planos ativos: %', (SELECT COUNT(*) FROM platform_plans WHERE active = true);
END $$;
