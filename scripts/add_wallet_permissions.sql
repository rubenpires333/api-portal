-- ============================================================================
-- Script para adicionar permissões de Wallet
-- ============================================================================

-- Inserir permissões de Wallet (se não existirem)
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Visualizar Carteira', 'wallet.view', 'Permite visualizar carteira e transações', 'wallet', 'view', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'wallet.view');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Solicitar Levantamento', 'wallet.withdraw', 'Permite solicitar levantamentos da carteira', 'wallet', 'withdraw', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'wallet.withdraw');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Visualizar Taxas', 'billing.fees.read', 'Permite visualizar regras de taxas de levantamento', 'billing', 'read', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'billing.fees.read');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Gerenciar Billing', 'billing.manage', 'Permite gerenciar planos, taxas e levantamentos', 'billing', 'manage', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'billing.manage');

-- Associar permissões ao SUPER_ADMIN (todas as permissões)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE code = 'SUPER_ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN ('wallet.view', 'wallet.withdraw', 'billing.fees.read', 'billing.manage')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = (SELECT id FROM roles WHERE code = 'SUPER_ADMIN')
    AND rp.permission_id = p.id
);

-- Associar permissões ao PROVIDER (visualizar, solicitar levantamentos e ver taxas)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE code = 'PROVIDER'),
    p.id
FROM permissions p
WHERE p.code IN ('wallet.view', 'wallet.withdraw', 'billing.fees.read')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = (SELECT id FROM roles WHERE code = 'PROVIDER')
    AND rp.permission_id = p.id
);

-- Verificar permissões criadas
SELECT 
    p.code,
    p.name,
    p.description,
    r.code as role_code,
    r.name as role_name
FROM permissions p
LEFT JOIN role_permissions rp ON p.id = rp.permission_id
LEFT JOIN roles r ON rp.role_id = r.id
WHERE p.code IN ('wallet.view', 'wallet.withdraw', 'billing.fees.read', 'billing.manage')
ORDER BY p.code, r.code;
