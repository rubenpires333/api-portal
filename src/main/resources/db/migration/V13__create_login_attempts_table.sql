-- Criar tabela de tentativas de login
CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_attempt TIMESTAMP NOT NULL,
    blocked_until TIMESTAMP,
    requires_captcha BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(email, ip_address)
);

-- Criar índices para melhor performance
CREATE INDEX idx_login_attempts_email ON login_attempts(email);
CREATE INDEX idx_login_attempts_ip ON login_attempts(ip_address);
CREATE INDEX idx_login_attempts_last_attempt ON login_attempts(last_attempt);
CREATE INDEX idx_login_attempts_blocked_until ON login_attempts(blocked_until) WHERE blocked_until IS NOT NULL;

-- Comentários
COMMENT ON TABLE login_attempts IS 'Tabela para rastrear tentativas de login e implementar proteção contra brute force';
COMMENT ON COLUMN login_attempts.email IS 'Email do usuário que tentou fazer login';
COMMENT ON COLUMN login_attempts.ip_address IS 'Endereço IP da tentativa';
COMMENT ON COLUMN login_attempts.attempts IS 'Número de tentativas falhadas';
COMMENT ON COLUMN login_attempts.last_attempt IS 'Data/hora da última tentativa';
COMMENT ON COLUMN login_attempts.blocked_until IS 'Data/hora até quando o usuário está bloqueado';
COMMENT ON COLUMN login_attempts.requires_captcha IS 'Indica se CAPTCHA é necessário (após 3 tentativas)';
