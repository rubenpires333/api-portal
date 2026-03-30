-- Adicionar campos de uso de requisicoes nas subscricoes
ALTER TABLE subscriptions 
ADD COLUMN IF NOT EXISTS requests_used INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS requests_limit INTEGER;

-- Atualizar subscricoes existentes com valores default
UPDATE subscriptions 
SET requests_used = 0 
WHERE requests_used IS NULL;
