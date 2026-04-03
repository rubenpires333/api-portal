-- ============================================================================
-- Migration: Converter consumer_id de VARCHAR para UUID na tabela subscriptions
-- Data: 2026-04-03
-- Descrição: Corrige o tipo da coluna consumer_id para UUID
-- ============================================================================

-- PASSO 1: Verificar o tipo atual da coluna
SELECT 
    column_name, 
    data_type, 
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'subscriptions' AND column_name = 'consumer_id';

-- PASSO 2: Alterar o tipo da coluna consumer_id de VARCHAR para UUID
-- IMPORTANTE: Isso só funciona se todos os valores já forem UUIDs válidos
ALTER TABLE subscriptions 
ALTER COLUMN consumer_id TYPE uuid USING consumer_id::uuid;

-- PASSO 3: Verificar a alteração
SELECT 
    column_name, 
    data_type, 
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'subscriptions' AND column_name = 'consumer_id';

-- PASSO 4: Recriar índices se necessário
REINDEX INDEX idx_subscription_consumer;

-- PASSO 5: Verificar subscriptions existentes
SELECT 
    id,
    consumer_id,
    consumer_name,
    consumer_email,
    status
FROM subscriptions
LIMIT 5;

SELECT 'Migration concluída com sucesso! consumer_id agora é UUID.' as status;

