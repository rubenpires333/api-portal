-- =====================================================
-- Corrigir Transações Faltantes de Platform Subscriptions
-- =====================================================
-- Este script cria transações DEBIT_PLATFORM_FEE para
-- subscriptions ativas que não têm transação registrada
-- =====================================================

-- Inserir transações faltantes para subscriptions ativas
INSERT INTO wallet_transactions (
    id,
    wallet_id,
    type,
    amount,
    description,
    reference_id,
    status,
    created_at,
    updated_at
)
SELECT 
    gen_random_uuid() as id,
    pw.id as wallet_id,
    'DEBIT_PLATFORM_FEE' as type,
    pp.monthly_price as amount,
    CONCAT('Subscription fee - ', pp.display_name) as description,
    pps.id as reference_id,
    'COMPLETED' as status,
    pps.created_at as created_at,
    NOW() as updated_at
FROM provider_platform_subscriptions pps
JOIN platform_plans pp ON pps.plan_id = pp.id
JOIN provider_wallets pw ON pw.provider_id = pps.provider_id
WHERE pps.status = 'active'
  AND pp.monthly_price > 0
  AND NOT EXISTS (
      SELECT 1 
      FROM wallet_transactions wt 
      WHERE wt.reference_id = pps.id 
        AND wt.type = 'DEBIT_PLATFORM_FEE'
  );

-- Verificar transações criadas
SELECT 
    'TRANSAÇÕES CRIADAS' as tipo,
    COUNT(*) as quantidade,
    SUM(amount) as valor_total
FROM wallet_transactions
WHERE type = 'DEBIT_PLATFORM_FEE';

-- Listar transações criadas
SELECT 
    wt.id,
    wt.type,
    wt.amount,
    wt.description,
    wt.created_at,
    u.username as provider
FROM wallet_transactions wt
JOIN provider_wallets pw ON wt.wallet_id = pw.id
JOIN users u ON pw.provider_id = u.id
WHERE wt.type = 'DEBIT_PLATFORM_FEE'
ORDER BY wt.created_at DESC;

-- Mensagem de sucesso
DO $$
DECLARE
    v_count INTEGER;
    v_total DECIMAL(10,2);
BEGIN
    SELECT COUNT(*), COALESCE(SUM(amount), 0)
    INTO v_count, v_total
    FROM wallet_transactions
    WHERE type = 'DEBIT_PLATFORM_FEE';
    
    RAISE NOTICE '=========================================';
    RAISE NOTICE '✅ TRANSAÇÕES CORRIGIDAS COM SUCESSO!';
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'Total de transações DEBIT_PLATFORM_FEE: %', v_count;
    RAISE NOTICE 'Valor total: % EUR', v_total;
    RAISE NOTICE '=========================================';
END $$;
