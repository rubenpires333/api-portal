-- ============================================================================
-- Script para verificar o status da carteira
-- ============================================================================

-- Verificar se a carteira existe
SELECT 
    pw.id as wallet_id,
    pw.provider_id,
    u.email,
    pw.available_balance,
    pw.pending_balance,
    pw.reserved_balance,
    pw.lifetime_earned,
    pw.currency,
    pw.minimum_payout,
    pw.created_at,
    pw.updated_at
FROM provider_wallets pw
JOIN users u ON pw.provider_id = u.id
WHERE pw.provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';

-- Verificar as transações
SELECT 
    wt.id,
    wt.wallet_id,
    wt.amount,
    wt.type,
    wt.status,
    wt.description,
    wt.available_at,
    wt.created_at
FROM wallet_transactions wt
JOIN provider_wallets pw ON wt.wallet_id = pw.id
WHERE pw.provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2'
ORDER BY wt.created_at DESC;

-- Contar quantas carteiras existem para este provider
SELECT COUNT(*) as total_wallets
FROM provider_wallets
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
