-- ============================================
-- RESETAR CARTEIRA PARA TESTES
-- ============================================
-- ⚠️ ATENÇÃO: Este script apaga TODOS os dados de billing/carteira
-- Use apenas em ambiente de DESENVOLVIMENTO/TESTE

-- ============================================
-- BACKUP ANTES DE APAGAR (OPCIONAL)
-- ============================================
-- Descomente para fazer backup antes de apagar
/*
CREATE TABLE backup_withdrawal_requests AS SELECT * FROM withdrawal_requests;
CREATE TABLE backup_wallet_transactions AS SELECT * FROM wallet_transactions;
CREATE TABLE backup_provider_wallets AS SELECT * FROM provider_wallets;
CREATE TABLE backup_checkout_sessions AS SELECT * FROM checkout_sessions;
CREATE TABLE backup_platform_subscriptions AS SELECT * FROM platform_subscriptions;
*/

-- ============================================
-- 1. VERIFICAR DADOS ANTES DE APAGAR
-- ============================================
SELECT '=== DADOS ATUAIS ===' as info;

SELECT 'Provider Wallets' as tabela, COUNT(*) as registros FROM provider_wallets
UNION ALL
SELECT 'Wallet Transactions', COUNT(*) FROM wallet_transactions
UNION ALL
SELECT 'Withdrawal Requests', COUNT(*) FROM withdrawal_requests
UNION ALL
SELECT 'Checkout Sessions', COUNT(*) FROM checkout_sessions
UNION ALL
SELECT 'Platform Subscriptions', COUNT(*) FROM platform_subscriptions;

-- ============================================
-- 2. APAGAR DADOS (ORDEM CORRETA - FOREIGN KEYS)
-- ============================================

-- 2.1. Apagar levantamentos (depende de wallet)
DELETE FROM withdrawal_requests;
SELECT 'Withdrawal requests apagados' as status;

-- 2.2. Apagar transações da carteira (depende de wallet)
DELETE FROM wallet_transactions;
SELECT 'Wallet transactions apagadas' as status;

-- 2.3. Apagar sessões de checkout (depende de subscription)
DELETE FROM checkout_sessions;
SELECT 'Checkout sessions apagadas' as status;

-- 2.4. Apagar subscriptions de plataforma
DELETE FROM platform_subscriptions;
SELECT 'Platform subscriptions apagadas' as status;

-- 2.5. Apagar carteiras dos providers
DELETE FROM provider_wallets;
SELECT 'Provider wallets apagadas' as status;

-- ============================================
-- 3. RESETAR SEQUENCES (OPCIONAL)
-- ============================================
-- Se houver sequences, resetar para começar do 1
-- ALTER SEQUENCE nome_sequence RESTART WITH 1;

-- ============================================
-- 4. VERIFICAR LIMPEZA
-- ============================================
SELECT '=== APÓS LIMPEZA ===' as info;

SELECT 'Provider Wallets' as tabela, COUNT(*) as registros FROM provider_wallets
UNION ALL
SELECT 'Wallet Transactions', COUNT(*) FROM wallet_transactions
UNION ALL
SELECT 'Withdrawal Requests', COUNT(*) FROM withdrawal_requests
UNION ALL
SELECT 'Checkout Sessions', COUNT(*) FROM checkout_sessions
UNION ALL
SELECT 'Platform Subscriptions', COUNT(*) FROM platform_subscriptions;

-- ============================================
-- 5. CRIAR CARTEIRA LIMPA PARA PROVIDER DE TESTE
-- ============================================
-- Descomente para criar carteira zerada para o provider

INSERT INTO provider_wallets (
    id,
    provider_id,
    available_balance,
    reserved_balance,
    pending_balance,
    currency,
    minimum_payout,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    '69f2020f-7e2a-42ba-bf32-5821cfebe0c2',  -- Provider ID
    0.00,                                      -- Saldo disponível zerado
    0.00,                                      -- Saldo reservado zerado
    0.00,                                      -- Saldo pendente zerado
    'EUR',                                     -- Moeda
    200.00,                                    -- Mínimo para levantamento
    NOW(),
    NOW()
) ON CONFLICT (provider_id) DO UPDATE SET
    available_balance = 0.00,
    reserved_balance = 0.00,
    pending_balance = 0.00,
    updated_at = NOW();

SELECT 'Carteira criada/resetada para provider' as status;

-- ============================================
-- 6. VERIFICAR CARTEIRA CRIADA
-- ============================================
SELECT 
    provider_id,
    available_balance,
    reserved_balance,
    pending_balance,
    currency,
    minimum_payout,
    created_at
FROM provider_wallets
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';

-- ============================================
-- 7. INFORMAÇÕES PARA TESTE
-- ============================================
SELECT '=== PRONTO PARA TESTAR ===' as info;
SELECT 'Agora você pode:' as instrucao
UNION ALL SELECT '1. Fazer subscription via Stripe Checkout'
UNION ALL SELECT '2. Webhook irá processar pagamento'
UNION ALL SELECT '3. Sistema irá:'
UNION ALL SELECT '   - Criar/atualizar carteira do provider'
UNION ALL SELECT '   - Registrar receita da plataforma (100% do valor)'
UNION ALL SELECT '   - Criar transação na carteira'
UNION ALL SELECT '   - Atualizar saldos corretamente'
UNION ALL SELECT '4. Verificar com: SELECT * FROM provider_wallets'
UNION ALL SELECT '5. Verificar transações: SELECT * FROM wallet_transactions';

-- ============================================
-- EXEMPLO DE TESTE
-- ============================================
/*
-- Após executar este script:

1. Fazer subscription:
   curl -X POST "http://localhost:8080/api/v1/billing/checkout/create-session" \
     -H "Content-Type: application/json" \
     -d '{
       "providerId": "69f2020f-7e2a-42ba-bf32-5821cfebe0c2",
       "planName": "GROWTH",
       "successUrl": "http://localhost:4200/success",
       "cancelUrl": "http://localhost:4200/cancel"
     }'

2. Completar pagamento no Stripe (usar cartão de teste: 4242 4242 4242 4242)

3. Webhook processa automaticamente

4. Verificar carteira:
   SELECT * FROM provider_wallets 
   WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
   
   Esperado:
   - available_balance: 0.00 (plataforma fica com 100%)
   - pending_balance: 0.00
   - reserved_balance: 0.00

5. Verificar transações:
   SELECT * FROM wallet_transactions 
   WHERE wallet_id = (SELECT id FROM provider_wallets 
                      WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2')
   ORDER BY created_at DESC;
   
   Esperado:
   - 1 transação de PLATFORM_SUBSCRIPTION_REVENUE
   - Valor: 49.00 EUR (plano GROWTH)
   - Status: AVAILABLE
   - Description: "Receita de subscription de plataforma - GROWTH"

6. Verificar subscription:
   SELECT * FROM platform_subscriptions 
   WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2'
   ORDER BY created_at DESC;
   
   Esperado:
   - Status: ACTIVE
   - Plan: GROWTH
   - Amount: 49.00
*/
