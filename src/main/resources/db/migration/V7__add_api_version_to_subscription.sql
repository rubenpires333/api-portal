-- Adicionar coluna api_version_id à tabela subscriptions
ALTER TABLE subscriptions ADD COLUMN api_version_id UUID;

-- Criar índice para melhorar performance
CREATE INDEX idx_subscription_version ON subscriptions(api_version_id);

-- Atualizar subscriptions existentes com a versão padrão da API
UPDATE subscriptions s
SET api_version_id = (
    SELECT av.id
    FROM api_versions av
    WHERE av.api_id = s.api_id
    AND av.is_default = true
    LIMIT 1
)
WHERE s.api_version_id IS NULL;

-- Se não houver versão padrão, usar a primeira versão publicada
UPDATE subscriptions s
SET api_version_id = (
    SELECT av.id
    FROM api_versions av
    WHERE av.api_id = s.api_id
    AND av.status = 'PUBLISHED'
    ORDER BY av.created_at ASC
    LIMIT 1
)
WHERE s.api_version_id IS NULL;

-- Se ainda não houver versão, usar a primeira versão disponível
UPDATE subscriptions s
SET api_version_id = (
    SELECT av.id
    FROM api_versions av
    WHERE av.api_id = s.api_id
    ORDER BY av.created_at ASC
    LIMIT 1
)
WHERE s.api_version_id IS NULL;
