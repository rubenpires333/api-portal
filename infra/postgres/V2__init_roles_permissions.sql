-- =====================================================
-- Script de Inicialização de Roles e Permissões
-- =====================================================

-- Inserir Permissões Básicas
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at) VALUES
-- Permissões de API
(gen_random_uuid(), 'Criar API', 'api.create', 'Permite criar novas APIs', 'api', 'create', true, NOW(), NOW()),
(gen_random_uuid(), 'Ler API', 'api.read', 'Permite visualizar APIs', 'api', 'read', true, NOW(), NOW()),
(gen_random_uuid(), 'Atualizar API', 'api.update', 'Permite atualizar APIs', 'api', 'update', true, NOW(), NOW()),
(gen_random_uuid(), 'Deletar API', 'api.delete', 'Permite deletar APIs', 'api', 'delete', true, NOW(), NOW()),
(gen_random_uuid(), 'Publicar API', 'api.publish', 'Permite publicar APIs', 'api', 'publish', true, NOW(), NOW()),

-- Permissões de Categoria
(gen_random_uuid(), 'Criar Categoria', 'category.create', 'Permite criar categorias', 'category', 'create', true, NOW(), NOW()),
(gen_random_uuid(), 'Ler Categoria', 'category.read', 'Permite visualizar categorias', 'category', 'read', true, NOW(), NOW()),
(gen_random_uuid(), 'Atualizar Categoria', 'category.update', 'Permite atualizar categorias', 'category', 'update', true, NOW(), NOW()),
(gen_random_uuid(), 'Deletar Categoria', 'category.delete', 'Permite deletar categorias', 'category', 'delete', true, NOW(), NOW()),

-- Permissões de Usuário
(gen_random_uuid(), 'Gerenciar Usuários', 'user.manage', 'Permite gerenciar usuários', 'user', 'manage', true, NOW(), NOW()),
(gen_random_uuid(), 'Ler Usuário', 'user.read', 'Permite visualizar usuários', 'user', 'read', true, NOW(), NOW()),
(gen_random_uuid(), 'Atualizar Usuário', 'user.update', 'Permite atualizar usuários', 'user', 'update', true, NOW(), NOW()),

-- Permissões de Role
(gen_random_uuid(), 'Gerenciar Roles', 'role.manage', 'Permite gerenciar roles', 'role', 'manage', true, NOW(), NOW()),
(gen_random_uuid(), 'Ler Role', 'role.read', 'Permite visualizar roles', 'role', 'read', true, NOW(), NOW()),

-- Permissões de Permissão
(gen_random_uuid(), 'Gerenciar Permissões', 'permission.manage', 'Permite gerenciar permissões', 'permission', 'manage', true, NOW(), NOW()),
(gen_random_uuid(), 'Ler Permissão', 'permission.read', 'Permite visualizar permissões', 'permission', 'read', true, NOW(), NOW()),

-- Permissões de Auditoria
(gen_random_uuid(), 'Ler Auditoria', 'audit.read', 'Permite visualizar logs de auditoria', 'audit', 'read', true, NOW(), NOW());

-- Inserir Roles do Sistema
INSERT INTO roles (id, name, code, description, is_system, active, created_at, updated_at) VALUES
(gen_random_uuid(), 'Super Admin', 'SUPER_ADMIN', 'Administrador com acesso total ao sistema', true, true, NOW(), NOW()),
(gen_random_uuid(), 'Provider', 'PROVIDER', 'Provedor de APIs', true, true, NOW(), NOW()),
(gen_random_uuid(), 'Consumer', 'CONSUMER', 'Consumidor de APIs', true, true, NOW(), NOW());

-- Associar todas as permissões ao SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE code = 'SUPER_ADMIN'),
    id
FROM permissions;

-- Associar permissões ao PROVIDER
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE code = 'PROVIDER'),
    id
FROM permissions
WHERE code IN (
    'api.create',
    'api.read',
    'api.update',
    'api.delete',
    'api.publish',
    'version.create',
    'version.read',
    'version.update',
    'version.delete',
    'endpoint.create',
    'endpoint.read',
    'endpoint.update',
    'endpoint.delete',
    'category.read',
    'user.read'
);

-- Associar permissões ao CONSUMER
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM roles WHERE code = 'CONSUMER'),
    id
FROM permissions
WHERE code IN (
    'api.read',
    'category.read',
    'user.read'
);

-- Comentários
COMMENT ON TABLE permissions IS 'Tabela de permissões do sistema';
COMMENT ON TABLE roles IS 'Tabela de roles (papéis) do sistema';
COMMENT ON TABLE role_permissions IS 'Tabela de associação entre roles e permissões';
COMMENT ON TABLE users IS 'Tabela de usuários sincronizados do Keycloak';
COMMENT ON TABLE user_roles IS 'Tabela de associação entre usuários e roles';
