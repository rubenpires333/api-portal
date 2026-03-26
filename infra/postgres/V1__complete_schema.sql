-- ================================================================
--  API PORTAL — SCHEMA COMPLETO E SIMPLIFICADO (SINGLE-TENANT)
--  PostgreSQL 16+
-- ================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- BLOCO 1 — PLATAFORMA
CREATE TABLE platform_config (
    id                      UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                    VARCHAR(255)  NOT NULL DEFAULT 'API Portal',
    logo_url                VARCHAR(500),
    support_email           VARCHAR(255),
    terms_url               VARCHAR(500),
    privacy_url             VARCHAR(500),
    max_apis_per_provider   INTEGER       NOT NULL DEFAULT 50,
    max_subscriptions_free  INTEGER       NOT NULL DEFAULT 5,
    default_rate_limit      INTEGER       NOT NULL DEFAULT 1000,
    sandbox_enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    payments_enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    payment_gateway         VARCHAR(50)   NOT NULL DEFAULT 'STRIPE',
    maintenance_mode        BOOLEAN       NOT NULL DEFAULT FALSE,
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by              UUID
);
INSERT INTO platform_config DEFAULT VALUES;

-- BLOCO 2 — USERS (3 roles: SUPERADMIN / PROVIDER / CONSUMER)
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    keycloak_id     VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    name            VARCHAR(255),
    avatar_url      VARCHAR(500),
    role            VARCHAR(20)  NOT NULL DEFAULT 'CONSUMER',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_keycloak UNIQUE (keycloak_id),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('SUPERADMIN','PROVIDER','CONSUMER'))
);

-- BLOCO 3 — PROVIDERS (perfil detalhado do provedor)
CREATE TABLE providers (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL,
    company_name    VARCHAR(255) NOT NULL,
    description     TEXT,
    website         VARCHAR(500),
    logo_url        VARCHAR(500),
    support_email   VARCHAR(255),
    support_url     VARCHAR(500),
    country         VARCHAR(3),
    category        VARCHAR(100),
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at     TIMESTAMPTZ,
    verified_by     UUID,
    total_apis      INTEGER      NOT NULL DEFAULT 0,
    total_subs      INTEGER      NOT NULL DEFAULT 0,
    rating          DECIMAL(3,2),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_providers_user     UNIQUE (user_id),
    CONSTRAINT fk_providers_user     FOREIGN KEY (user_id)     REFERENCES users(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_providers_verified FOREIGN KEY (verified_by) REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT chk_providers_status  CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','REJECTED')),
    CONSTRAINT chk_providers_rating  CHECK (rating BETWEEN 0 AND 5)
);

-- BLOCO 4 — APIS
CREATE TABLE apis (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id     UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    short_desc      VARCHAR(500),
    description     TEXT,
    category        VARCHAR(100),
    tags            TEXT[],
    type            VARCHAR(20)  NOT NULL DEFAULT 'PROXY',
    status          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    visibility      VARCHAR(50)  NOT NULL DEFAULT 'PUBLIC',
    logo_url        VARCHAR(500),
    docs_url        VARCHAR(500),
    total_subs      INTEGER      NOT NULL DEFAULT 0,
    avg_latency_ms  INTEGER,
    uptime_pct      DECIMAL(5,2),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_apis_slug    UNIQUE (slug),
    CONSTRAINT fk_apis_provider FOREIGN KEY (provider_id) REFERENCES providers(id)  ON DELETE RESTRICT,
    CONSTRAINT chk_apis_type     CHECK (type       IN ('PROXY','NATIVE')),
    CONSTRAINT chk_apis_status   CHECK (status     IN ('DRAFT','PENDING_REVIEW','ACTIVE','DEPRECATED','INACTIVE')),
    CONSTRAINT chk_apis_visibility CHECK (visibility IN ('PUBLIC','PRIVATE'))
);

CREATE TABLE api_versions (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_id          UUID         NOT NULL,
    version         VARCHAR(20)  NOT NULL,
    openapi_spec    JSONB,
    changelog       TEXT,
    is_current      BOOLEAN      NOT NULL DEFAULT FALSE,
    status          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    sunset_date     DATE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_api_version  UNIQUE (api_id, version),
    CONSTRAINT fk_av_api       FOREIGN KEY (api_id) REFERENCES apis(id) ON DELETE CASCADE,
    CONSTRAINT chk_av_status   CHECK (status IN ('DRAFT','ACTIVE','DEPRECATED'))
);

CREATE TABLE api_reviews (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_id      UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    rating      SMALLINT     NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_user_api  UNIQUE (api_id, user_id),
    CONSTRAINT fk_review_api       FOREIGN KEY (api_id)  REFERENCES apis(id)  ON DELETE CASCADE,
    CONSTRAINT fk_review_user      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_review_rating   CHECK (rating BETWEEN 1 AND 5)
);

-- BLOCO 5 — PROXY E CREDENCIAIS DO PROVEDOR
CREATE TABLE third_party_apis (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_id            UUID         NOT NULL,
    base_url          VARCHAR(500) NOT NULL,
    sandbox_base_url  VARCHAR(500),
    auth_type         VARCHAR(50)  NOT NULL DEFAULT 'API_KEY',
    timeout_ms        INTEGER      NOT NULL DEFAULT 5000,
    retry_enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    max_retries       SMALLINT     NOT NULL DEFAULT 2,
    forward_headers   BOOLEAN      NOT NULL DEFAULT FALSE,
    strip_prefix      VARCHAR(100),
    health_check_url  VARCHAR(500),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tpa_api       UNIQUE (api_id),
    CONSTRAINT fk_tpa_api       FOREIGN KEY (api_id) REFERENCES apis(id) ON DELETE CASCADE,
    CONSTRAINT chk_tpa_auth     CHECK (auth_type IN ('NONE','API_KEY','BEARER','BASIC','OAUTH2')),
    CONSTRAINT chk_tpa_timeout  CHECK (timeout_ms BETWEEN 500 AND 60000),
    CONSTRAINT chk_tpa_retries  CHECK (max_retries BETWEEN 0 AND 5)
);

CREATE TABLE third_party_credentials (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    third_party_api_id  UUID         NOT NULL,
    credential_key      VARCHAR(100) NOT NULL,
    credential_value    TEXT         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tpc_key UNIQUE (third_party_api_id, credential_key),
    CONSTRAINT fk_tpc_tpa FOREIGN KEY (third_party_api_id) REFERENCES third_party_apis(id) ON DELETE CASCADE
);

-- BLOCO 6 — PLANOS E POLÍTICAS
CREATE TABLE plans (
    id              UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_id          UUID          NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'USD',
    billing_cycle   VARCHAR(20)   NOT NULL DEFAULT 'MONTH',
    is_free         BOOLEAN       GENERATED ALWAYS AS (price = 0) STORED,
    is_public       BOOLEAN       NOT NULL DEFAULT TRUE,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    trial_days      INTEGER       NOT NULL DEFAULT 0,
    features        JSONB,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_plans_api     FOREIGN KEY (api_id) REFERENCES apis(id) ON DELETE CASCADE,
    CONSTRAINT chk_plans_price  CHECK (price >= 0),
    CONSTRAINT chk_plans_cycle  CHECK (billing_cycle IN ('MONTH','YEAR','ONE_TIME'))
);

CREATE TABLE usage_policies (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id         UUID         NOT NULL,
    rate_limit      INTEGER      NOT NULL DEFAULT 1000,
    rate_period     VARCHAR(20)  NOT NULL DEFAULT 'MONTH',
    burst_limit     INTEGER,
    daily_quota     BIGINT,
    monthly_quota   BIGINT,
    max_payload_kb  INTEGER      NOT NULL DEFAULT 1024,
    allowed_ips     TEXT[],
    allowed_methods TEXT[]       NOT NULL DEFAULT '{GET,POST,PUT,DELETE,PATCH}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_policy_plan    UNIQUE (plan_id),
    CONSTRAINT fk_policy_plan    FOREIGN KEY (plan_id)    REFERENCES plans(id)      ON DELETE CASCADE,
    CONSTRAINT chk_policy_period CHECK (rate_period IN ('MINUTE','HOUR','DAY','WEEK','MONTH'))
);

CREATE TABLE sla_policies (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id             UUID         NOT NULL,
    uptime_sla_pct      DECIMAL(5,2) NOT NULL DEFAULT 99.00,
    max_latency_ms      INTEGER      NOT NULL DEFAULT 2000,
    support_level       VARCHAR(50)  NOT NULL DEFAULT 'COMMUNITY',
    support_response_h  INTEGER      NOT NULL DEFAULT 72,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sla_plan     UNIQUE (plan_id),
    CONSTRAINT fk_sla_plan     FOREIGN KEY (plan_id)    REFERENCES plans(id)      ON DELETE CASCADE,
    CONSTRAINT chk_sla_support CHECK (support_level IN ('COMMUNITY','EMAIL','PRIORITY','DEDICATED'))
);

-- BLOCO 7 — SUBSCRIÇÕES
CREATE TABLE subscriptions (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    consumer_id     UUID         NOT NULL,
    plan_id         UUID         NOT NULL,
    api_key         VARCHAR(128) NOT NULL DEFAULT encode(gen_random_bytes(32),'hex'),
    api_key_prefix  VARCHAR(8)   GENERATED ALWAYS AS (LEFT(api_key,8)) STORED,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    environment     VARCHAR(20)  NOT NULL DEFAULT 'PRODUCTION',
    trial_ends_at   TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    cancel_reason   VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sub_api_key   UNIQUE (api_key),
    CONSTRAINT fk_sub_consumer  FOREIGN KEY (consumer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_plan      FOREIGN KEY (plan_id)     REFERENCES plans(id) ON DELETE RESTRICT,
    CONSTRAINT chk_sub_status   CHECK (status      IN ('TRIAL','ACTIVE','SUSPENDED','CANCELLED','EXPIRED')),
    CONSTRAINT chk_sub_env      CHECK (environment IN ('PRODUCTION','SANDBOX'))
);

CREATE TABLE api_key_rotations (
    id              BIGSERIAL    PRIMARY KEY,
    subscription_id UUID         NOT NULL,
    old_key_prefix  VARCHAR(8),
    reason          VARCHAR(255),
    rotated_by      UUID,
    rotated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_akr_sub    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
);

-- BLOCO 8 — SANDBOX
CREATE TABLE sandbox_environments (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_id          UUID         NOT NULL,
    base_url        VARCHAR(500),
    mode            VARCHAR(20)  NOT NULL DEFAULT 'MOCK',
    mock_delay_ms   INTEGER      NOT NULL DEFAULT 200,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sandbox_api UNIQUE (api_id),
    CONSTRAINT fk_sandbox_api FOREIGN KEY (api_id) REFERENCES apis(id) ON DELETE CASCADE,
    CONSTRAINT chk_sandbox_mode CHECK (mode IN ('MOCK','FORWARD','RECORD_REPLAY'))
);

CREATE TABLE sandbox_mock_responses (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    sandbox_id       UUID         NOT NULL,
    method           VARCHAR(10)  NOT NULL,
    path_pattern     VARCHAR(500) NOT NULL,
    status_code      INTEGER      NOT NULL DEFAULT 200,
    response_body    JSONB,
    response_headers JSONB,
    description      VARCHAR(255),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_smr_sandbox FOREIGN KEY (sandbox_id) REFERENCES sandbox_environments(id) ON DELETE CASCADE,
    CONSTRAINT chk_smr_method CHECK (method IN ('GET','POST','PUT','DELETE','PATCH','HEAD','OPTIONS'))
);

CREATE TABLE sandbox_test_runs (
    id              BIGSERIAL    PRIMARY KEY,
    subscription_id UUID,
    api_id          UUID,
    method          VARCHAR(10),
    path            VARCHAR(500),
    request_body    JSONB,
    response_body   JSONB,
    status_code     INTEGER,
    duration_ms     INTEGER,
    ran_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_str_sub    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE SET NULL,
    CONSTRAINT fk_str_api    FOREIGN KEY (api_id)          REFERENCES apis(id)          ON DELETE SET NULL
);

-- BLOCO 9 — PAGAMENTOS
CREATE TABLE payment_methods (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID         NOT NULL,
    gateway             VARCHAR(50)  NOT NULL DEFAULT 'STRIPE',
    gateway_customer_id VARCHAR(100),
    gateway_method_id   VARCHAR(100) NOT NULL,
    type                VARCHAR(50)  NOT NULL DEFAULT 'CARD',
    brand               VARCHAR(50),
    last_four           VARCHAR(4),
    exp_month           SMALLINT,
    exp_year            SMALLINT,
    is_default          BOOLEAN      NOT NULL DEFAULT FALSE,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pm_gateway_method UNIQUE (gateway_method_id),
    CONSTRAINT fk_pm_user           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_pm_type          CHECK (type IN ('CARD','VINTI4','BANK_TRANSFER','PAYPAL'))
);

CREATE TABLE invoices (
    id                      UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id         UUID          NOT NULL,
    payment_method_id       UUID,
    gateway                 VARCHAR(50)   NOT NULL DEFAULT 'STRIPE',
    gateway_invoice_id      VARCHAR(100),
    gateway_payment_intent  VARCHAR(100),
    amount                  DECIMAL(10,2) NOT NULL,
    amount_paid             DECIMAL(10,2) NOT NULL DEFAULT 0,
    currency                VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status                  VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    description             TEXT,
    period_start            TIMESTAMPTZ,
    period_end              TIMESTAMPTZ,
    due_at                  TIMESTAMPTZ,
    paid_at                 TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inv_sub     FOREIGN KEY (subscription_id)   REFERENCES subscriptions(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_inv_pm      FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE SET NULL,
    CONSTRAINT chk_inv_status CHECK (status IN ('PENDING','PAID','OVERDUE','VOID','REFUNDED','FAILED'))
);

CREATE TABLE payment_webhooks (
    id            BIGSERIAL    PRIMARY KEY,
    gateway       VARCHAR(50)  NOT NULL DEFAULT 'STRIPE',
    event_id      VARCHAR(100) NOT NULL,
    event_type    VARCHAR(100) NOT NULL,
    payload       JSONB        NOT NULL,
    processed     BOOLEAN      NOT NULL DEFAULT FALSE,
    processed_at  TIMESTAMPTZ,
    error_message TEXT,
    received_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_webhook_event UNIQUE (event_id)
);

-- BLOCO 10 — ANALYTICS
CREATE TABLE api_usage (
    id              BIGSERIAL    PRIMARY KEY,
    subscription_id UUID,
    api_id          UUID,
    api_version_id  UUID,
    method          VARCHAR(10),
    endpoint        VARCHAR(500),
    status_code     INTEGER,
    response_ms     INTEGER,
    bytes_out       INTEGER,
    bytes_in        INTEGER,
    ip_address      INET,
    environment     VARCHAR(20)  NOT NULL DEFAULT 'PRODUCTION',
    called_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_usage_sub     FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE SET NULL,
    CONSTRAINT fk_usage_api     FOREIGN KEY (api_id)          REFERENCES apis(id)           ON DELETE SET NULL,
    CONSTRAINT fk_usage_version FOREIGN KEY (api_version_id)  REFERENCES api_versions(id)   ON DELETE SET NULL,
    CONSTRAINT chk_usage_env    CHECK (environment IN ('PRODUCTION','SANDBOX'))
);

CREATE TABLE proxy_request_log (
    id                  BIGSERIAL    PRIMARY KEY,
    third_party_api_id  UUID,
    subscription_id     UUID,
    method              VARCHAR(10),
    endpoint            VARCHAR(500),
    status_code         INTEGER,
    upstream_ms         INTEGER,
    retry_count         SMALLINT     NOT NULL DEFAULT 0,
    error_message       TEXT,
    environment         VARCHAR(20)  NOT NULL DEFAULT 'PRODUCTION',
    called_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_prl_tpa    FOREIGN KEY (third_party_api_id) REFERENCES third_party_apis(id) ON DELETE SET NULL,
    CONSTRAINT fk_prl_sub    FOREIGN KEY (subscription_id)    REFERENCES subscriptions(id)   ON DELETE SET NULL
);

-- BLOCO 11 — GOVERNANÇA
CREATE TABLE approval_requests (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    type          VARCHAR(50)  NOT NULL,
    entity_id     UUID         NOT NULL,
    requested_by  UUID         NOT NULL,
    reviewed_by   UUID,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    notes         TEXT,
    reviewed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ar_requested_by  FOREIGN KEY (requested_by) REFERENCES users(id)     ON DELETE RESTRICT,
    CONSTRAINT fk_ar_reviewed_by   FOREIGN KEY (reviewed_by)  REFERENCES users(id)     ON DELETE SET NULL,
    CONSTRAINT chk_ar_type   CHECK (type   IN ('PROVIDER_REGISTRATION','API_PUBLISH','API_VERSION','PLAN_CHANGE')),
    CONSTRAINT chk_ar_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
);

CREATE TABLE audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     UUID,
    role        VARCHAR(20),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(100),
    old_value   JSONB,
    new_value   JSONB,
    ip_address  INET,
    user_agent  VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID,
    type        VARCHAR(100) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT,
    read        BOOLEAN      NOT NULL DEFAULT FALSE,
    action_url  VARCHAR(500),
    metadata    JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notif_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ÍNDICES
CREATE INDEX idx_users_keycloak       ON users(keycloak_id);
CREATE INDEX idx_users_role           ON users(role);
CREATE INDEX idx_providers_user       ON providers(user_id);
CREATE INDEX idx_providers_status     ON providers(status);
CREATE INDEX idx_providers_verified   ON providers(verified) WHERE verified = TRUE;
CREATE INDEX idx_apis_provider        ON apis(provider_id);
CREATE INDEX idx_apis_tags            ON apis USING GIN (tags);
CREATE INDEX idx_apis_name_trgm       ON apis USING GIN (name gin_trgm_ops);
CREATE INDEX idx_av_api               ON api_versions(api_id);
CREATE INDEX idx_av_current           ON api_versions(api_id, is_current) WHERE is_current = TRUE;
CREATE INDEX idx_plans_api            ON plans(api_id);
CREATE INDEX idx_sub_consumer         ON subscriptions(consumer_id);
CREATE INDEX idx_sub_api_key          ON subscriptions(api_key);
CREATE INDEX idx_sandbox_api          ON sandbox_environments(api_id);
CREATE INDEX idx_smr_sandbox          ON sandbox_mock_responses(sandbox_id);
CREATE INDEX idx_pm_user              ON payment_methods(user_id);
CREATE INDEX idx_inv_sub              ON invoices(subscription_id);
CREATE INDEX idx_usage_api            ON api_usage(api_id, called_at DESC);
CREATE INDEX idx_usage_sub            ON api_usage(subscription_id, called_at DESC);
CREATE INDEX idx_prl_error            ON proxy_request_log(status_code) WHERE status_code >= 400;
CREATE INDEX idx_notif_unread         ON notifications(user_id, read) WHERE read = FALSE;

-- DADOS INICIAIS
INSERT INTO users (id, keycloak_id, email, name, role) VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin', 'admin@apiportal.local', 'Super Admin', 'SUPERADMIN');
