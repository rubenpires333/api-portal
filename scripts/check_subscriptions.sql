-- Verificar assinaturas criadas
SELECT 
    id,
    provider_id,
    plan_id,
    stripe_subscription_id,
    stripe_customer_id,
    status,
    current_period_start,
    current_period_end,
    created_at
FROM provider_platform_subscriptions
ORDER BY created_at DESC
LIMIT 10;

-- Verificar webhooks recebidos
SELECT 
    event_id,
    event_type,
    gateway_type,
    processed,
    created_at
FROM payment_webhooks
ORDER BY created_at DESC
LIMIT 10;

-- Verificar planos disponíveis
SELECT 
    id,
    name,
    display_name,
    monthly_price,
    stripe_price_id
FROM platform_plans
WHERE active = true
ORDER BY display_order;
