-- Adicionar coluna last_reset_at para rastrear último reset do contador
ALTER TABLE subscriptions 
ADD COLUMN last_reset_at TIMESTAMP;

-- Inicializar com a data de criação da subscription
UPDATE subscriptions 
SET last_reset_at = created_at 
WHERE last_reset_at IS NULL;
