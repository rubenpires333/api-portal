-- =====================================================
-- Debug Platform Wallet - Verificar Receitas
-- =====================================================

-- 1. Verificar subscriptions ativas
SELECT 
    'SUBSCRIPTIONS ATIVAS' as tipo,
    COUNT(*) as quantidade,
    SUM(p.monthly_price) as valor_total
FROM provider_platform_subscriptions pps
JOIN platform_plans p ON pps.plan_id = p.id
WHERE pps.status = 'active';

-- 2. Verificar transações da wallet
SELECT 
    'TRANSAÇÕES WALLET' as tipo,
    type as tipo_transacao,
    COUNT(*) as quantidade,
    SUM(amount) as valor_total
FROM wallet_transactions
GROUP BY type;

-- 3. Verificar se há wallet_transactions de DEBIT_PLATFORM_FEE
SELECT 
    'RECEITA SUBSCRIPTIONS' as tipo,
    COUNT(*) as quantidade,
    SUM(amount) as valor_total
FROM wallet_transactions
WHERE type = 'DEBIT_PLATFORM_FEE';

-- 4. Verificar comissões API
SELECT 
    'COMISSÕES API' as tipo,
    COUNT(*) as quantidade,
    SUM(amount) as valor_total
FROM wallet_transactions
WHERE type = 'CREDIT_REVENUE';

-- 5. Listar todas as subscriptions
SELECT 
    pps.id,
    pps.provider_id,
    p.name as plano,
    p.monthly_price,
    p.currency,
    pps.status,
    pps.created_at
FROM provider_platform_subscriptions pps
JOIN platform_plans p ON pps.plan_id = p.id
ORDER BY pps.created_at DESC
LIMIT 10;

-- 6. Listar últimas transações
SELECT 
    wt.id,
    wt.type,
    wt.amount,
    wt.description,
    wt.created_at,
    wt.reference_id
FROM wallet_transactions wt
ORDER BY wt.created_at DESC
LIMIT 10;

-- 7. Verificar checkout sessions
SELECT 
    cs.id,
    cs.status,
    cs.payment_method,
    cs.amount,
    cs.currency,
    cs.created_at,
    cs.expires_at
FROM checkout_sessions cs
WHERE cs.type = 'platform_subscription'
ORDER BY cs.created_at DESC
LIMIT 10;

-- Resumo
SELECT '========================================' as '';
SELECT 'RESUMO DO DEBUG' as '';
SELECT '========================================' as '';
SELECT 
    CONCAT('Subscriptions Ativas: ', COUNT(*)) as info
FROM provider_platform_subscriptions 
WHERE status = 'active'
UNION ALL
SELECT 
    CONCAT('Transações Wallet: ', COUNT(*))
FROM wallet_transactions
UNION ALL
SELECT 
    CONCAT('Receita Subscriptions: ', COALESCE(SUM(amount), 0), ' EUR')
FROM wallet_transactions
WHERE type = 'DEBIT_PLATFORM_FEE'
UNION ALL
SELECT 
    CONCAT('Comissões API: ', COALESCE(SUM(amount), 0), ' EUR')
FROM wallet_transactions
WHERE type = 'CREDIT_REVENUE';
