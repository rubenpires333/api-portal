-- =====================================================
-- Script para Adicionar Permissões de Versões e Endpoints
-- Execute este script manualmente no banco de dados
-- =====================================================

-- Inserir Permissões de Versão (se não existirem)
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Criar Versão', 'version.create', 'Permite criar versões de APIs', 'version', 'create', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'version.create');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Ler Versão', 'version.read', 'Permite visualizar versões de APIs', 'version', 'read', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'version.read');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Atualizar Versão', 'version.update', 'Permite atualizar versões de APIs', 'version', 'update', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'version.update');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Deletar Versão', 'version.delete', 'Permite deletar versões de APIs', 'version', 'delete', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'version.delete');

-- Inserir Permissões de Endpoint (se não existirem)
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Criar Endpoint', 'endpoint.create', 'Permite criar endpoints de APIs', 'endpoint', 'create', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'endpoint.create');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Ler Endpoint', 'endpoint.read', 'Permite visualizar endpoints de APIs', 'endpoint', 'read', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'endpoint.read');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Atualizar Endpoint', 'endpoint.update', 'Permite atualizar endpoints de APIs', 'endpoint', 'update', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'endpoint.update');

INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Deletar Endpoint', 'endpoint.delete', 'Permite deletar endpoints de APIs', 'endpoint', 'delete', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'endpoint.delete');

-- Associar permissões ao SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE code = 'SUPER_ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN (
    'version.create', 'version.read', 'version.update', 'version.delete',
    'endpoint.create', 'endpoint.read', 'endpoint.update', 'endpoint.delete'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = (SELECT id FROM roles WHERE code = 'SUPER_ADMIN')
    AND rp.permission_id = p.id
);

-- Associar permissões ao PROVIDER
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE code = 'PROVIDER'),
    p.id
FROM permissions p
WHERE p.code IN (
    'version.create', 'version.read', 'version.update', 'version.delete',
    'endpoint.create', 'endpoint.read', 'endpoint.update', 'endpoint.delete'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = (SELECT id FROM roles WHERE code = 'PROVIDER')
    AND rp.permission_id = p.id
);

-- Verificar permissões adicionadas
SELECT 'Permissões adicionadas com sucesso!' as status;
SELECT r.name as role, p.name as permission, p.code
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.code LIKE 'version.%' OR p.code LIKE 'endpoint.%'
ORDER BY r.name, p.code;
