-- Gateway Configurations
CREATE TABLE gateway_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gateway_type VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT false,
    display_name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(500),
    settings JSONB,
    supported_currencies VARCHAR(100),
    supports_subscriptions BOOLEAN NOT NULL DEFAULT false,
    supports_refunds BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Platform Plans (Starter, Growth, Business)
CREATE TABLE platform_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    monthly_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    max_apis INTEGER,
    max_requests_per_month INTEGER,
    max_team_members INTEGER,
    custom_domain BOOLEAN DEFAULT false,
    priority_support BOOLEAN DEFAULT false,
    advanced_analytics BOOLEAN DEFAULT false,
    stripe_price_id VARCHAR(100),
    vinti4_price_id VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Provider Platform Subscriptions
CREATE TABLE provider_platform_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES platform_plans(id),
    status VARCHAR(50) NOT NULL,
    gateway_subscription_id VARCHAR(200),
    current_price DECIMAL(10,2),
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Provider Wallets
CREATE TABLE provider_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL UNIQUE,
    available_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    pending_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    reserved_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    lifetime_earned DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    preferred_method VARCHAR(50),
    payout_details TEXT,
    minimum_payout DECIMAL(10,2) DEFAULT 10.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Wallet Transactions
CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES provider_wallets(id),
    amount DECIMAL(15,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reference_id UUID,
    description TEXT,
    available_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallet_transactions_wallet ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_transactions_status ON wallet_transactions(status);
CREATE INDEX idx_wallet_transactions_available_at ON wallet_transactions(available_at);

-- Revenue Share Events
CREATE TABLE revenue_share_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID,
    provider_id UUID NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    platform_commission_percentage DECIMAL(5,2) NOT NULL,
    platform_commission DECIMAL(15,2) NOT NULL,
    provider_share DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    wallet_transaction_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_revenue_share_provider ON revenue_share_events(provider_id);

-- Withdrawal Requests
CREATE TABLE withdrawal_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES provider_wallets(id),
    requested_amount DECIMAL(15,2) NOT NULL,
    fee_percentage DECIMAL(5,2) NOT NULL,
    fee_amount DECIMAL(15,2) NOT NULL,
    net_amount DECIMAL(15,2) NOT NULL,
    method VARCHAR(50) NOT NULL,
    destination_details TEXT,
    status VARCHAR(50) NOT NULL,
    approved_by UUID,
    rejection_reason TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_withdrawal_requests_wallet ON withdrawal_requests(wallet_id);
CREATE INDEX idx_withdrawal_requests_status ON withdrawal_requests(status);

-- Withdrawal Fee Rules
CREATE TABLE withdrawal_fee_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    withdrawal_method VARCHAR(50) NOT NULL UNIQUE,
    fee_percentage DECIMAL(5,2) NOT NULL,
    fixed_fee DECIMAL(10,2) NOT NULL,
    fixed_fee_currency VARCHAR(3) NOT NULL,
    minimum_amount DECIMAL(10,2),
    maximum_amount DECIMAL(15,2),
    active BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID
);

-- Payment Webhooks (for idempotency)
CREATE TABLE payment_webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(200) NOT NULL UNIQUE,
    gateway_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    processed BOOLEAN NOT NULL DEFAULT false,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_payment_webhooks_event_id ON payment_webhooks(event_id);

-- Insert default platform plans
INSERT INTO platform_plans (name, display_name, description, monthly_price, currency, max_apis, max_requests_per_month, max_team_members, custom_domain, priority_support, advanced_analytics, active)
VALUES 
    ('STARTER', 'Starter', 'Perfect for getting started', 0.00, 'USD', 3, 10000, 1, false, false, false, true),
    ('GROWTH', 'Growth', 'For growing businesses', 49.00, 'USD', 10, 100000, 5, true, false, true, true),
    ('BUSINESS', 'Business', 'For established businesses', 149.00, 'USD', NULL, NULL, NULL, true, true, true, true);

-- Insert default withdrawal fee rules
INSERT INTO withdrawal_fee_rules (withdrawal_method, fee_percentage, fixed_fee, fixed_fee_currency, minimum_amount, active)
VALUES 
    ('VINTI4', 1.50, 200.00, 'CVE', 1000.00, true),
    ('BANK_TRANSFER', 2.00, 500.00, 'CVE', 2000.00, true),
    ('PAYPAL', 3.00, 1.00, 'USD', 10.00, true),
    ('WISE', 2.50, 0.50, 'USD', 10.00, true),
    ('PLATFORM_CREDIT', 0.00, 0.00, 'USD', 0.00, true);

-- Insert default gateway config (Stripe)
INSERT INTO gateway_configs (gateway_type, active, display_name, supported_currencies, supports_subscriptions, supports_refunds)
VALUES 
    ('STRIPE', true, 'Stripe', 'USD,EUR,GBP', true, true),
    ('VINTI4', false, 'Vinti4', 'CVE', true, false);
