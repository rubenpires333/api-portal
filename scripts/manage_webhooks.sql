-- ============================================
-- GERENCIAMENTO DE WEBHOOKS
-- ============================================

-- 1. Ver estatísticas de webhooks
SELECT 
    gateway_type,
    event_type,
    processed,
    COUNT(*) as total,
    COUNT(DISTINCT event_id) as unique_events,
    COUNT(*) - COUNT(DISTINCT event_id) as duplicates
FROM payment_webhooks
GROUP BY gateway_type, event_type, processed
ORDER BY gateway_type, event_type;

-- 2. Ver webhooks duplicados (mesmo event_id)
SELECT 
    event_id,
    event_type,
    COUNT(*) as occurrences,
    MIN(received_at) as first_received,
    MAX(received_at) as last_received,
    MAX(received_at) - MIN(received_at) as time_diff
FROM payment_webhooks
GROUP BY event_id, event_type
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC, last_received DESC;

-- 3. Ver webhooks não processados (possíveis erros)
SELECT 
    id,
    event_id,
    gateway_type,
    event_type,
    received_at,
    processed_at
FROM payment_webhooks
WHERE processed = false
ORDER BY received_at DESC
LIMIT 50;

-- 4. Limpar webhooks duplicados (manter apenas o primeiro)
-- CUIDADO: Execute apenas se tiver certeza
WITH duplicates AS (
    SELECT 
        id,
        event_id,
        ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY received_at ASC) as rn
    FROM payment_webhooks
)
DELETE FROM payment_webhooks
WHERE id IN (
    SELECT id FROM duplicates WHERE rn > 1
);

-- 5. Limpar webhooks antigos processados (>90 dias)
DELETE FROM payment_webhooks
WHERE processed = true
AND received_at < NOW() - INTERVAL '90 days';

-- 6. Reprocessar webhook específico
-- Marcar como não processado para Stripe reenviar
UPDATE payment_webhooks
SET processed = false,
    processed_at = NULL
WHERE event_id = 'evt_XXXXXXXXXXXXXXXX';

-- 7. Ver taxa de sucesso por tipo de evento
SELECT 
    event_type,
    COUNT(*) as total,
    SUM(CASE WHEN processed THEN 1 ELSE 0 END) as processed,
    ROUND(100.0 * SUM(CASE WHEN processed THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate
FROM payment_webhooks
GROUP BY event_type
ORDER BY total DESC;

-- 8. Ver tempo médio de processamento
SELECT 
    event_type,
    COUNT(*) as total,
    AVG(EXTRACT(EPOCH FROM (processed_at - received_at))) as avg_processing_seconds,
    MAX(EXTRACT(EPOCH FROM (processed_at - received_at))) as max_processing_seconds
FROM payment_webhooks
WHERE processed = true
AND processed_at IS NOT NULL
GROUP BY event_type
ORDER BY avg_processing_seconds DESC;

-- 9. Ver webhooks recentes (últimas 24h)
SELECT 
    event_id,
    gateway_type,
    event_type,
    processed,
    received_at,
    processed_at,
    EXTRACT(EPOCH FROM (processed_at - received_at)) as processing_seconds
FROM payment_webhooks
WHERE received_at > NOW() - INTERVAL '24 hours'
ORDER BY received_at DESC;

-- 10. Criar índices para performance (se não existirem)
CREATE INDEX IF NOT EXISTS idx_webhook_event_id ON payment_webhooks(event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_processed ON payment_webhooks(processed);
CREATE INDEX IF NOT EXISTS idx_webhook_received_at ON payment_webhooks(received_at);
CREATE INDEX IF NOT EXISTS idx_webhook_event_type ON payment_webhooks(event_type);
