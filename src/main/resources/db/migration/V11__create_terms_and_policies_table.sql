-- Criar tabela de termos e políticas
CREATE TABLE IF NOT EXISTS terms_and_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    terms_of_service TEXT NOT NULL,
    privacy_policy TEXT NOT NULL,
    version VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT false,
    updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Criar índice para buscar versão ativa rapidamente
CREATE INDEX idx_terms_and_policies_active ON terms_and_policies(is_active) WHERE is_active = true;

-- Inserir dados iniciais
INSERT INTO terms_and_policies (
    terms_of_service,
    privacy_policy,
    version,
    is_active,
    created_at,
    updated_at
) VALUES (
    '<h2>Termos de Serviço</h2><p>Bem-vindo à nossa plataforma. Estes são os termos de serviço padrão.</p>',
    '<h2>Política de Privacidade</h2><p>Esta é a política de privacidade padrão da plataforma.</p>',
    '1.0.0',
    true,
    NOW(),
    NOW()
);
