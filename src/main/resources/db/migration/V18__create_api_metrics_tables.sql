-- Tabela principal de métricas de API
CREATE TABLE api_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_id UUID NOT NULL REFERENCES apis(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    consumer_id UUID,  -- Alterado de VARCHAR(255) para UUID
    consumer_name VARCHAR(255),
    endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    status_code INT NOT NULL,
    response_time_ms DOUBLE PRECISION NOT NULL,
    request_size_bytes BIGINT DEFAULT 0,
    response_size_bytes BIGINT DEFAULT 0,
    error_message TEXT,
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_api_metrics_api_id (api_id),
    INDEX idx_api_metrics_subscription_id (subscription_id),
    INDEX idx_api_metrics_consumer_id (consumer_id),
    INDEX idx_api_metrics_created_at (created_at),
    INDEX idx_api_metrics_status_code (status_code),
    INDEX idx_api_metrics_api_created (api_id, created_at)
);

-- Tabela de métricas agregadas diárias (para performance)
CREATE TABLE api_metrics_daily (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_id UUID NOT NULL REFERENCES apis(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    total_calls BIGINT NOT NULL DEFAULT 0,
    success_calls BIGINT NOT NULL DEFAULT 0,
    error_calls BIGINT NOT NULL DEFAULT 0,
    avg_response_time DOUBLE PRECISION NOT NULL DEFAULT 0,
    min_response_time DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_response_time DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_request_size BIGINT NOT NULL DEFAULT 0,
    total_response_size BIGINT NOT NULL DEFAULT 0,
    unique_consumers INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE (api_id, metric_date),
    INDEX idx_api_metrics_daily_api_id (api_id),
    INDEX idx_api_metrics_daily_date (metric_date)
);

-- Comentários
COMMENT ON TABLE api_metrics IS 'Registro detalhado de cada chamada às APIs';
COMMENT ON TABLE api_metrics_daily IS 'Métricas agregadas por dia para otimização de consultas';
COMMENT ON COLUMN api_metrics.response_time_ms IS 'Tempo de resposta em milissegundos';
COMMENT ON COLUMN api_metrics.status_code IS 'HTTP status code da resposta';
COMMENT ON COLUMN api_metrics_daily.metric_date IS 'Data das métricas agregadas';
