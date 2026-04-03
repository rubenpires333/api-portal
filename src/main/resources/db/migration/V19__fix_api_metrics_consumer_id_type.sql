-- Migration para corrigir o tipo de consumer_id de VARCHAR para UUID
-- Esta migration é necessária se a V18 já foi aplicada com o tipo errado

-- Verificar se a coluna existe e tem o tipo errado
DO $$
BEGIN
    -- Verificar se a coluna consumer_id existe e é VARCHAR
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'api_metrics' 
        AND column_name = 'consumer_id' 
        AND data_type = 'character varying'
    ) THEN
        -- Limpar dados existentes se houver (para evitar problemas de conversão)
        TRUNCATE TABLE api_metrics_daily CASCADE;
        TRUNCATE TABLE api_metrics CASCADE;
        
        -- Alterar o tipo da coluna para UUID
        ALTER TABLE api_metrics 
        ALTER COLUMN consumer_id TYPE UUID USING consumer_id::UUID;
        
        RAISE NOTICE 'Coluna consumer_id alterada de VARCHAR para UUID com sucesso';
    ELSE
        RAISE NOTICE 'Coluna consumer_id já é UUID ou não existe';
    END IF;
END $$;

-- Comentário
COMMENT ON COLUMN api_metrics.consumer_id IS 'UUID do consumer que fez a chamada à API';
