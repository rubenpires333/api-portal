-- ============================================================================
-- Script para recalcular os saldos da carteira baseado nas transações
-- ============================================================================

-- Recalcular saldos para o provider específico
UPDATE provider_wallets pw
SET 
    available_balance = COALESCE((
        SELECT SUM(wt.amount)
        FROM wallet_transactions wt
        WHERE wt.wallet_id = pw.id
        AND wt.status = 'AVAILABLE'
    ), 0),
    pending_balance = COALESCE((
        SELECT SUM(wt.amount)
        FROM wallet_transactions wt
        WHERE wt.wallet_id = pw.id
        AND wt.status = 'PENDING'
    ), 0),
    reserved_balance = COALESCE((
        SELECT SUM(wt.amount)
        FROM wallet_transactions wt
        WHERE wt.wallet_id = pw.id
        AND wt.status = 'RESERVED'
    ), 0),
    lifetime_earned = COALESCE((
        SELECT SUM(wt.amount)
        FROM wallet_transactions wt
        WHERE wt.wallet_id = pw.id
        AND wt.type LIKE 'CREDIT_%'
        AND wt.amount > 0
    ), 0),
    updated_at = NOW()
WHERE pw.provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';

-- Verificar resultado
SELECT 
    pw.provider_id,
    u.email,
    pw.available_balance,
    pw.pending_balance,
    pw.reserved_balance,
    pw.lifetime_earned,
    pw.currency,
    pw.minimum_payout,
    CASE 
        WHEN pw.available_balance >= pw.minimum_payout THEN 'SIM - Pode levantar!'
        ELSE 'NÃO - Saldo insuficiente'
    END as pode_levantar
FROM provider_wallets pw
JOIN users u ON pw.provider_id = u.id
WHERE pw.provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';

-- Mostrar transações
SELECT 
    wt.type,
    wt.status,
    wt.amount,
    wt.description,
    wt.created_at
FROM wallet_transactions wt
JOIN provider_wallets pw ON wt.wallet_id = pw.id
WHERE pw.provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2'
ORDER BY wt.created_at DESC;
