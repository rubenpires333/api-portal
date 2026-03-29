-- Script para adicionar campos de autenticação na tabela api_endpoints
-- Execute este script manualmente no banco de dados

-- Adicionar colunas de autenticação
ALTER TABLE api_endpoints 
ADD COLUMN IF NOT EXISTS auth_headers_json TEXT,
ADD COLUMN IF NOT EXISTS auth_query_params_json TEXT;

-- Comentários
COMMENT ON COLUMN api_endpoints.auth_headers_json IS 'JSON com headers de autenticação necessários (ex: [{"key":"Authorization","value":"Bearer {token}"}])';
COMMENT ON COLUMN api_endpoints.auth_query_params_json IS 'JSON com query parameters de autenticação (ex: [{"key":"api_key","value":"{api_key}"}])';
