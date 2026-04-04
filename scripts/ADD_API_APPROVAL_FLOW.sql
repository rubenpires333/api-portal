-- Migration: Adicionar fluxo de aprovação de APIs
-- Descrição: Adiciona novos status e campos para controle de aprovação de APIs pelo administrador

-- Remover constraint antiga de status
ALTER TABLE apis DROP CONSTRAINT IF EXISTS apis_status_check;

-- Adicionar constraint com novos status
ALTER TABLE apis ADD CONSTRAINT apis_status_check 
CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED', 'DEPRECATED', 'ARCHIVED'));

-- Adicionar novos campos na tabela apis
ALTER TABLE apis 
ADD COLUMN IF NOT EXISTS requested_approval_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS approved_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Comentários nos campos
COMMENT ON COLUMN apis.requested_approval_at IS 'Data/hora em que a aprovação foi solicitada';
COMMENT ON COLUMN apis.approved_at IS 'Data/hora em que a API foi aprovada';
COMMENT ON COLUMN apis.approved_by IS 'Keycloak ID do admin que aprovou';
COMMENT ON COLUMN apis.rejected_at IS 'Data/hora em que a API foi rejeitada';
COMMENT ON COLUMN apis.rejected_by IS 'Keycloak ID do admin que rejeitou';
COMMENT ON COLUMN apis.rejection_reason IS 'Motivo da rejeição';

-- Criar índices para melhorar performance de consultas
CREATE INDEX IF NOT EXISTS idx_apis_status_requested_approval 
ON apis(status, requested_approval_at DESC) 
WHERE status = 'PENDING_APPROVAL';

CREATE INDEX IF NOT EXISTS idx_apis_approved_at 
ON apis(approved_at DESC) 
WHERE approved_at IS NOT NULL;

-- Adicionar permissão para admin gerenciar aprovações de APIs
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'Gerenciar Aprovações de APIs', 'admin.apis.manage', 'Gerenciar aprovações de APIs', 'admin.apis', 'manage', true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Atribuir permissão ao role ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN' 
  AND p.code = 'admin.apis.manage'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Log de execução
DO $$
BEGIN
    RAISE NOTICE 'Migration ADD_API_APPROVAL_FLOW executada com sucesso!';
    RAISE NOTICE 'Novos status disponíveis: PENDING_APPROVAL, REJECTED';
    RAISE NOTICE 'Campos de aprovação adicionados à tabela apis';
    RAISE NOTICE 'Permissão admin.apis.manage criada e atribuída ao role ADMIN';
END $$;
