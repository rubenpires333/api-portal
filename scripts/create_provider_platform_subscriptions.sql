-- Criar tabela de assinaturas de plataforma dos providers
CREATE TABLE IF NOT EXISTS provider_platform_subscriptions (
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

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_provider_platform_sub_provider ON provider_platform_subscriptions(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_platform_sub_stripe ON provider_platform_subscriptions(stripe_subscription_id);
CREATE INDEX IF NOT EXISTS idx_provider_platform_sub_status ON provider_platform_subscriptions(status);

-- Comentários
COMMENT ON TABLE provider_platform_subscriptions IS 'Assinaturas dos providers aos planos da plataforma';
COMMENT ON COLUMN provider_platform_subscriptions.status IS 'Status: active, canceled, past_due, unpaid, trialing';
