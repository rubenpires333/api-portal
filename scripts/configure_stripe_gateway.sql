-- =====================================================
-- Configurar Gateway Stripe
-- =====================================================

-- Inserir ou atualizar configuração do Stripe
INSERT INTO gateway_configs (
    id,
    gateway_type,
    display_name,
    logo_url,
    active,
    supported_currencies,
    supports_subscriptions,
    supports_refunds,
    settings,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'STRIPE',
    'Stripe',
    'https://stripe.com/img/v3/home/social.png',
    true,
    ARRAY['USD', 'EUR', 'GBP', 'BRL', 'CVE'],
    true,
    true,
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (gateway_type) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    active = true,
    supported_currencies = EXCLUDED.supported_currencies,
    supports_subscriptions = EXCLUDED.supports_subscriptions,
    supports_refunds = EXCLUDED.supports_refunds,
    updated_at = NOW();

-- Verificar configuração
SELECT 
    gateway_type,
    display_name,
    active,
    supported_currencies,
    supports_subscriptions,
    supports_refunds
FROM gateway_configs
WHERE gateway_type = 'STRIPE';

-- Mensagem de sucesso
DO $$
BEGIN
    RAISE NOTICE '=========================================';
    RAISE NOTICE '✅ GATEWAY STRIPE CONFIGURADO!';
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'Gateway ativo: %', (SELECT active FROM gateway_configs WHERE gateway_type = 'STRIPE');
END $$;
