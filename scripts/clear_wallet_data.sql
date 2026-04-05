-- ============================================
-- Script para limpar dados da Wallet
-- ============================================
-- ATENÇÃO: Este script remove TODOS os dados de wallet, transações e subscriptions
-- Use apenas em ambiente de TESTE!
-- ============================================

-- 1. Limpar transações da wallet
DELETE FROM wallet_transactions;

-- 2. Limpar subscriptions da plataforma
DELETE FROM provider_platform_subscriptions;

-- 3. Limpar checkout sessions
DELETE FROM checkout_sessions;

-- 4. Limpar levantamentos
DELETE FROM withdrawal_requests;

-- 5. Resetar wallets (zerar saldos)
UPDATE provider_wallets 
SET available_balance = 0.00,
    pending_balance = 0.00,
    reserved_balance = 0.00,
    updated_at = NOW();

-- Verificar resultados
SELECT 'Wallet Transactions' as tabela, COUNT(*) as registros FROM wallet_transactions
UNION ALL
SELECT 'Provider Platform Subscriptions', COUNT(*) FROM provider_platform_subscriptions
UNION ALL
SELECT 'Checkout Sessions', COUNT(*) FROM checkout_sessions
UNION ALL
SELECT 'Withdrawal Requests', COUNT(*) FROM withdrawal_requests
UNION ALL
SELECT 'Provider Wallets (com saldo)', COUNT(*) FROM provider_wallets 
WHERE available_balance > 0 OR pending_balance > 0 OR reserved_balance > 0;
