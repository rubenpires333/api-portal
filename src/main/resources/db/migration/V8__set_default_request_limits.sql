-- Definir limite de requisições baseado no rate_limit da API
UPDATE subscriptions s
SET requests_limit = COALESCE(
        (SELECT a.rate_limit FROM apis a WHERE a.id = s.api_id),
        1000
    ),
    requests_used = COALESCE(s.requests_used, 0)
WHERE s.requests_limit IS NULL OR s.requests_limit = 1000;
