-- ============================================================================
-- FORÇAR ATUALIZAÇÃO DE PERMISSÕES CONSUMER
-- Data: 2026-04-03
-- Descrição: Script para garantir que todas as permissões CONSUMER estejam corretas
-- ============================================================================

-- PASSO 1: Garantir que a permissão consumer.apis.read existe
-- ============================================================================
INSERT INTO permissions (id, code, name, description, resource, action, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'consumer.apis.read',
    'Visualizar Minhas APIs',
    'Permite visualizar APIs com subscrição ativa',
    'CONSUMER_APIS',
    'READ',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) 
DO UPDATE SET
    active = true,
    updated_at = CURRENT_TIMESTAMP;

-- PASSO 2: Garantir que a permissão consumer.metrics.read existe
-- ============================================================================
INSERT INTO permissions (id, code, name, description, resource, action, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'consumer.metrics.read',
    'Visualizar Estatísticas de Uso',
    'Permite visualizar métricas e estatísticas de uso das APIs',
    'CONSUMER_METRICS',
    'READ',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) 
DO UPDATE SET
    active = true,
    updated_at = CURRENT_TIMESTAMP;

-- PASSO 3: Garantir que o role CONSUMER existe e está ativo
-- ============================================================================
UPDATE roles 
SET active = true, updated_at = CURRENT_TIMESTAMP
WHERE code = 'CONSUMER';

-- PASSO 4: Associar consumer.apis.read ao role CONSUMER
-- ============================================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'CONSUMER'
  AND p.code = 'consumer.apis.read'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- PASSO 5: Associar consumer.metrics.read ao role CONSUMER
-- ============================================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'CONSUMER'
  AND p.code = 'consumer.metrics.read'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- PASSO 6: Verificar resultado
-- ============================================================================
SELECT 
    '✅ PERMISSÕES CONSUMER CONFIGURADAS' as status,
    COUNT(*) as total_permissoes
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'CONSUMER'
  AND p.code LIKE 'consumer.%';

-- Listar todas as permissões do CONSUMER
SELECT 
    r.code as role_code,
    p.code as permission_code,
    p.name as permission_name,
    p.active as active
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'CONSUMER'
ORDER BY p.code;

-- ============================================================================
-- IMPORTANTE: Após executar este script, você DEVE:
-- 1. Fazer LOGOUT no frontend
-- 2. Fazer LOGIN novamente
-- 3. O sistema irá recarregar as permissões do banco de dados
-- ============================================================================
