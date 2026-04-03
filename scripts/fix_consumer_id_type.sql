-- ============================================================================
-- Script para Corrigir o Tipo de consumer_id de VARCHAR para UUID
-- ============================================================================
-- Execute este script se você recebeu o erro:
-- "operator does not exist: uuid = character varying"
-- ============================================================================

-- ATENÇÃO: Este script irá LIMPAR todos os dados de métricas existentes!
-- Se você tem dados importantes, faça backup antes de executar.

BEGIN;

-- Verificar o tipo atual
SELECT 
    'Tipo atual de consumer_id: ' || data_type as info
FROM information_schema.columns 
WHERE table_name = 'api_metrics' 
AND column_name = 'consumer_id';

-- Limpar dados existentes (necessário para conversão segura)
TRUNCATE TABLE api_metrics_daily CASCADE;
TRUNCATE TABLE api_metrics CASCADE;

-- Alterar o tipo da coluna
ALTER TABLE api_metrics 
ALTER COLUMN consumer_id TYPE UUID USING consumer_id::UUID;

-- Verificar o novo tipo
SELECT 
    'Novo tipo de consumer_id: ' || data_type as info
FROM information_schema.columns 
WHERE table_name = 'api_metrics' 
AND column_name = 'consumer_id';

COMMIT;

SELECT '✅ Correção aplicada com sucesso!' as status;
SELECT 'Agora você pode executar os scripts de população de métricas.' as proximos_passos;
