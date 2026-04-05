-- Dropar tabela se existir (cuidado em produção!)
DROP TABLE IF EXISTS provider_platform_subscriptions CASCADE;

-- Recriar tabela com estrutura correta
CREATE TABLE provider_platform_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES platform_plans(id),
    stripe_subscription_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_provider FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_provider_subscription UNIQUE (provider_id)
);

-- Índices
CREATE INDEX idx_provider_platform_sub_provider ON provider_platform_subscriptions(provider_id);
CREATE INDEX idx_provider_platform_sub_stripe ON provider_platform_subscriptions(stripe_subscription_id);
CREATE INDEX idx_provider_platform_sub_status ON provider_platform_subscriptions(status);

-- Verificar
SELECT * FROM provider_platform_subscriptions;
