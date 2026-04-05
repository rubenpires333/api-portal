-- Script para testar sistema de Wallet + Holdback
-- Execute este script após fazer um pagamento de subscrição

-- ============================================
-- 1. VERIFICAR SUBSCRIÇÕES ATIVAS
-- ============================================
SELECT 
    pps.id,
    pps.provider_id,
    pp.name as plan_name,
    pp.monthly_price,
    pps.status,
    pps.current_period_start,
    pps.current_period_end,
    pps.created_at
FROM provider_platform_subscriptions pps
JOIN platform_plans pp ON pps.plan_id = pp.id
WHERE pps.status = 'active'
ORDER BY pps.created_at DESC;

-- ============================================
-- 2. VERIFICAR RECEITAS REGISTRADAS
-- ============================================
SELECT 
    id,
    provider_id,
    total_amount,
    platform_commission,
    provider_share,
    currency,
    created_at
FROM revenue_share_events
ORDER BY created_at DESC
LIMIT 10;

-- ============================================
-- 3. VERIFICAR CHECKOUT SESSIONS COMPLETADAS
-- ============================================
SELECT 
    id,
    provider_id,
    plan_name,
    amount,
    currency,
    status,
    stripe_payment_intent_id,
    completed_at,
    created_at
FROM checkout_sessions
WHERE status = 'COMPLETED'
ORDER BY created_at DESC
LIMIT 10;

-- ============================================
-- 4. VERIFICAR WALLETS DOS PROVIDERS
-- ============================================
SELECT 
    id,
    provider_id,
    available_balance,
    pending_balance,
    reserved_balance,
    lifetime_earned,
    currency,
    created_at,
    updated_at
FROM provider_wallets
ORDER BY updated_at DESC;

-- ============================================
-- 5. VERIFICAR TRANSAÇÕES DE WALLET
-- ============================================
SELECT 
    wt.id,
    pw.provider_id,
    wt.amount,
    wt.type,
    wt.status,
    wt.description,
    wt.available_at,
    wt.created_at,
    CASE 
        WHEN wt.available_at <= NOW() THEN 'Pronto para liberar'
        ELSE 'Em holdback'
    END as holdback_status
FROM wallet_transactions wt
JOIN provider_wallets pw ON wt.wallet_id = pw.id
ORDER BY wt.created_at DESC
LIMIT 20;

-- ============================================
-- 6. CRIAR TRANSAÇÃO DE TESTE (HOLDBACK EXPIRADO)
-- ============================================
-- Descomente para criar uma transação de teste

/*
DO $$
DECLARE
    v_wallet_id UUID;
    v_provider_id UUID;
BEGIN
    -- Pegar primeira wallet disponível
    SELECT id, provider_id INTO v_wallet_id, v_provider_id
    FROM provider_wallets
    LIMIT 1;
    
    IF v_wallet_id IS NOT NULL THEN
        -- Criar transação com holdback expirado
        INSERT INTO wallet_transactions (
            id, 
            wallet_id, 
            amount, 
            type, 
            status, 
            reference_id, 
            description, 
            available_at, 
            created_at
        ) VALUES (
            gen_random_uuid(),
            v_wallet_id,
            50.00,
            'CREDIT_REVENUE',
            'PENDING',
            gen_random_uuid(),
            'Test transaction - holdback expired',
            NOW() - INTERVAL '1 day',  -- Já passou do holdback
            NOW() - INTERVAL '15 days'
        );
        
        -- Atualizar pending balance
        UPDATE provider_wallets 
        SET 
            pending_balance = pending_balance + 50.00,
            lifetime_earned = lifetime_earned + 50.00,
            updated_at = NOW()
        WHERE id = v_wallet_id;
        
        RAISE NOTICE 'Transação de teste criada para provider: %', v_provider_id;
        RAISE NOTICE 'Wallet ID: %', v_wallet_id;
        RAISE NOTICE 'Aguarde 1 minuto para o job processar...';
    ELSE
        RAISE NOTICE 'Nenhuma wallet encontrada. Faça um pagamento primeiro.';
    END IF;
END $$;
*/

-- ============================================
-- 7. VERIFICAR TRANSAÇÕES PRONTAS PARA LIBERAR
-- ============================================
SELECT 
    wt.id,
    pw.provider_id,
    wt.amount,
    wt.type,
    wt.status,
    wt.description,
    wt.available_at,
    wt.created_at,
    NOW() - wt.available_at as tempo_desde_disponivel
FROM wallet_transactions wt
JOIN provider_wallets pw ON wt.wallet_id = pw.id
WHERE wt.status = 'PENDING'
  AND wt.available_at <= NOW()
ORDER BY wt.available_at ASC;

-- ============================================
-- 8. VERIFICAR HISTÓRICO DE LIBERAÇÕES
-- ============================================
SELECT 
    wt.id,
    pw.provider_id,
    wt.amount,
    wt.type,
    wt.status,
    wt.description,
    wt.available_at,
    wt.created_at
FROM wallet_transactions wt
JOIN provider_wallets pw ON wt.wallet_id = pw.id
WHERE wt.status = 'AVAILABLE'
ORDER BY wt.created_at DESC
LIMIT 20;

-- ============================================
-- 9. RESUMO POR PROVIDER
-- ============================================
SELECT 
    pw.provider_id,
    pw.available_balance,
    pw.pending_balance,
    pw.reserved_balance,
    pw.lifetime_earned,
    COUNT(wt.id) as total_transactions,
    COUNT(CASE WHEN wt.status = 'PENDING' THEN 1 END) as pending_transactions,
    COUNT(CASE WHEN wt.status = 'AVAILABLE' THEN 1 END) as available_transactions,
    SUM(CASE WHEN wt.status = 'PENDING' THEN wt.amount ELSE 0 END) as total_pending,
    SUM(CASE WHEN wt.status = 'AVAILABLE' THEN wt.amount ELSE 0 END) as total_available
FROM provider_wallets pw
LEFT JOIN wallet_transactions wt ON pw.id = wt.wallet_id
GROUP BY pw.id, pw.provider_id, pw.available_balance, pw.pending_balance, 
         pw.reserved_balance, pw.lifetime_earned
ORDER BY pw.updated_at DESC;

-- ============================================
-- 10. LIMPAR DADOS DE TESTE (CUIDADO!)
-- ============================================
-- Descomente apenas se quiser limpar TODOS os dados de teste

/*
-- ATENÇÃO: Isso vai apagar TODOS os dados de billing!
DELETE FROM wallet_transactions;
DELETE FROM revenue_share_events;
DELETE FROM provider_wallets;
DELETE FROM checkout_sessions WHERE status != 'PENDING';
DELETE FROM provider_platform_subscriptions;

RAISE NOTICE 'Dados de teste limpos!';
*/
