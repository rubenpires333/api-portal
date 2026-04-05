-- Quick Update: Fix Plan Descriptions
-- Copy and paste this into pgAdmin or any PostgreSQL client

-- Update Starter Plan
UPDATE platform_plans 
SET 
    description = 'Perfeito para começar e testar suas APIs',
    custom_domain = false,
    priority_support = false,
    advanced_analytics = false,
    updated_at = NOW()
WHERE name = 'STARTER';

-- Update Growth Plan
UPDATE platform_plans 
SET 
    description = 'Para desenvolvedores profissionais e pequenas equipes',
    custom_domain = false,
    priority_support = true,
    advanced_analytics = true,
    updated_at = NOW()
WHERE name = 'GROWTH';

-- Update Business Plan
UPDATE platform_plans 
SET 
    description = 'Para empresas com necessidades avançadas',
    custom_domain = true,
    priority_support = true,
    advanced_analytics = true,
    updated_at = NOW()
WHERE name = 'BUSINESS';

-- Verify the updates
SELECT 
    name,
    display_name,
    description,
    monthly_price,
    max_apis,
    max_requests_per_month,
    custom_domain,
    priority_support,
    advanced_analytics,
    active
FROM platform_plans
ORDER BY display_order;
