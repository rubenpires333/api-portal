-- Add display_order column to platform_plans table
ALTER TABLE platform_plans 
ADD COLUMN display_order INTEGER NOT NULL DEFAULT 1;

-- Update existing plans with sequential order
WITH numbered_plans AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) as rn
    FROM platform_plans
)
UPDATE platform_plans p
SET display_order = np.rn
FROM numbered_plans np
WHERE p.id = np.id;

-- Add comment
COMMENT ON COLUMN platform_plans.display_order IS 'Ordem de exibição dos planos (1, 2, 3...)';
