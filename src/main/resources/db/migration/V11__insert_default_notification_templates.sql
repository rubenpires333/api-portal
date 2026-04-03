-- Templates IN_APP em Português

INSERT INTO notification_templates (type, channel, language, subject, template, variables) VALUES
('SUBSCRIPTION_REQUESTED', 'IN_APP', 'pt', 
 'Nova Solicitação de Subscription',
 '{{consumerName}} solicitou subscription para a API {{apiName}}',
 '["consumerName", "consumerEmail", "apiName", "apiSlug"]'::jsonb),

('SUBSCRIPTION_APPROVED', 'IN_APP', 'pt',
 'Subscription Aprovada',
 'Sua subscription para a API {{apiName}} foi aprovada! Você já pode começar a usar.',
 '["apiName", "apiSlug", "apiKey"]'::jsonb),

('SUBSCRIPTION_REVOKED', 'IN_APP', 'pt',
 'Subscription Revogada',
 'Sua subscription para a API {{apiName}} foi revogada. Motivo: {{reason}}',
 '["apiName", "apiSlug", "reason"]'::jsonb),

('API_VERSION_RELEASED', 'IN_APP', 'pt',
 'Nova Versão Disponível',
 'A API {{apiName}} lançou a versão {{version}}. Confira as novidades!',
 '["apiName", "apiSlug", "version", "changelog"]'::jsonb),

('API_DEPRECATED', 'IN_APP', 'pt',
 'API Deprecated',
 'A API {{apiName}} foi marcada como deprecated. Considere migrar para alternativas.',
 '["apiName", "apiSlug", "deprecationDate"]'::jsonb),

('RATE_LIMIT_WARNING', 'IN_APP', 'pt',
 'Aviso de Limite',
 'Você usou {{usedRequests}} de {{limitRequests}} requisições ({{percentage}}%) da API {{apiName}}',
 '["apiName", "usedRequests", "limitRequests", "percentage"]'::jsonb),

('RATE_LIMIT_EXCEEDED', 'IN_APP', 'pt',
 'Limite Excedido',
 'Limite de requisições excedido para a API {{apiName}}. Aguarde o reset em {{resetDate}}.',
 '["apiName", "limitRequests", "resetDate"]'::jsonb);

-- Templates EMAIL em Português

INSERT INTO notification_templates (type, channel, language, subject, template, variables) VALUES
('SUBSCRIPTION_REQUESTED', 'EMAIL', 'pt',
 'Nova Solicitação de Subscription - {{apiName}}',
 '<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}.header{background:#4F46E5;color:white;padding:20px;text-align:center}.content{background:#f9f9f9;padding:20px}.button{display:inline-block;padding:12px 24px;background:#4F46E5;color:white;text-decoration:none;border-radius:4px}.footer{text-align:center;padding:20px;color:#666;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>API Portal</h2></div><div class="content"><h3>Nova Solicitação de Subscription</h3><p>Olá {{providerName}},</p><p><strong>{{consumerName}}</strong> ({{consumerEmail}}) solicitou subscription para sua API <strong>{{apiName}}</strong>.</p><p><a href="{{actionUrl}}" class="button">Ver Solicitação</a></p></div><div class="footer"><p>Esta é uma notificação automática do API Portal.</p></div></div></body></html>',
 '["providerName", "consumerName", "consumerEmail", "apiName", "actionUrl"]'::jsonb),

('SUBSCRIPTION_APPROVED', 'EMAIL', 'pt',
 'Subscription Aprovada - {{apiName}}',
 '<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}.header{background:#4F46E5;color:white;padding:20px;text-align:center}.content{background:#f9f9f9;padding:20px}.button{display:inline-block;padding:12px 24px;background:#4F46E5;color:white;text-decoration:none;border-radius:4px}.api-key{background:#fff;padding:15px;border-left:4px solid #10B981;margin:15px 0;font-family:monospace}.footer{text-align:center;padding:20px;color:#666;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>API Portal</h2></div><div class="content"><h3>🎉 Subscription Aprovada!</h3><p>Olá {{consumerName}},</p><p>Sua subscription para a API <strong>{{apiName}}</strong> foi aprovada!</p><div class="api-key"><strong>Sua API Key:</strong><br><code>{{apiKey}}</code></div><p><a href="{{actionUrl}}" class="button">Testar API Agora</a></p></div><div class="footer"><p>Guarde sua API Key em local seguro.</p></div></div></body></html>',
 '["consumerName", "apiName", "apiKey", "actionUrl"]'::jsonb),

('SUBSCRIPTION_REVOKED', 'EMAIL', 'pt',
 'Subscription Revogada - {{apiName}}',
 '<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}.header{background:#EF4444;color:white;padding:20px;text-align:center}.content{background:#f9f9f9;padding:20px}.footer{text-align:center;padding:20px;color:#666;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>API Portal</h2></div><div class="content"><h3>Subscription Revogada</h3><p>Olá {{consumerName}},</p><p>Sua subscription para a API <strong>{{apiName}}</strong> foi revogada.</p><p><strong>Motivo:</strong> {{reason}}</p><p>Se tiver dúvidas, entre em contato com o provider.</p></div><div class="footer"><p>API Portal - Gerenciamento de APIs</p></div></div></body></html>',
 '["consumerName", "apiName", "reason"]'::jsonb),

('API_VERSION_RELEASED', 'EMAIL', 'pt',
 'Nova Versão Disponível - {{apiName}} v{{version}}',
 '<!DOCTYPE html><html><head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}.header{background:#4F46E5;color:white;padding:20px;text-align:center}.content{background:#f9f9f9;padding:20px}.button{display:inline-block;padding:12px 24px;background:#4F46E5;color:white;text-decoration:none;border-radius:4px}.changelog{background:#fff;padding:15px;border-left:4px solid #3B82F6;margin:15px 0}.footer{text-align:center;padding:20px;color:#666;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>API Portal</h2></div><div class="content"><h3>🚀 Nova Versão Disponível</h3><p>Olá {{consumerName}},</p><p>A API <strong>{{apiName}}</strong> lançou a versão <strong>{{version}}</strong>!</p><div class="changelog"><strong>Novidades:</strong><br>{{changelog}}</div><p><a href="{{actionUrl}}" class="button">Ver Documentação</a></p></div><div class="footer"><p>API Portal</p></div></div></body></html>',
 '["consumerName", "apiName", "version", "changelog", "actionUrl"]'::jsonb);
