# Sistema de Notificações

## Objetivo
Implementar um sistema completo de notificações in-app e por email, com tipos configuráveis, templates personalizáveis e preferências do usuário.

## Funcionalidades Principais

### 1. Tipos de Notificações
- **SUBSCRIPTION_REQUESTED** - Nova solicitação de subscription (para Provider)
- **SUBSCRIPTION_APPROVED** - Subscription aprovada (para Consumer)
- **SUBSCRIPTION_REVOKED** - Subscription revogada (para Consumer)
- **API_VERSION_RELEASED** - Nova versão de API publicada (para Consumers subscritos)
- **API_DEPRECATED** - API marcada como deprecated (para Consumers subscritos)
- **RATE_LIMIT_WARNING** - Aviso de limite próximo (80% do limite)
- **RATE_LIMIT_EXCEEDED** - Limite de requisições excedido
- **API_MAINTENANCE** - Manutenção programada
- **API_INCIDENT** - Incidente reportado
- **PAYMENT_REQUIRED** - Pagamento pendente (se billing ativo)
- **PAYMENT_RECEIVED** - Pagamento confirmado

### 2. Canais de Notificação
- **IN_APP** - Notificação dentro da plataforma (sempre ativo)
- **EMAIL** - Notificação por email (configurável pelo usuário)

### 3. Templates de Email
- Templates HTML personalizáveis por tipo de notificação
- Variáveis dinâmicas: {{userName}}, {{apiName}}, {{actionUrl}}, etc.
- Suporte a múltiplos idiomas (PT, EN)

### 4. Preferências do Usuário
- Configurar quais tipos de notificação receber
- Escolher canais (in-app sempre ativo, email opcional)
- Frequência de digest (imediato, diário, semanal)

### 5. Interface
- Badge no ícone de sino no header com contador
- Dropdown com últimas notificações
- Página completa de notificações com filtros
- Marcar como lida/não lida
- Marcar todas como lidas
- Deletar notificações

## Estrutura de Dados

### Notification
- id (UUID)
- userId (String)
- type (NotificationType enum)
- title (String)
- message (String)
- data (JSON) - dados adicionais específicos do tipo
- isRead (Boolean)
- actionUrl (String) - link para ação relacionada
- createdAt (LocalDateTime)

### NotificationPreference
- id (UUID)
- userId (String)
- notificationType (NotificationType)
- inAppEnabled (Boolean) - sempre true
- emailEnabled (Boolean)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)

### NotificationTemplate
- id (UUID)
- type (NotificationType)
- channel (NotificationChannel enum)
- language (String)
- subject (String) - para email
- template (Text) - HTML para email, texto para in-app
- variables (JSON) - lista de variáveis disponíveis
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)

## Endpoints

### Consumer/Provider
- GET /api/v1/notifications - Listar notificações (paginado)
- GET /api/v1/notifications/unread-count - Contador de não lidas
- PATCH /api/v1/notifications/{id}/read - Marcar como lida
- PATCH /api/v1/notifications/mark-all-read - Marcar todas como lidas
- DELETE /api/v1/notifications/{id} - Deletar notificação

### Preferências
- GET /api/v1/notifications/preferences - Obter preferências
- PUT /api/v1/notifications/preferences - Atualizar preferências

### Admin
- GET /api/v1/admin/notification-templates - Listar templates
- POST /api/v1/admin/notification-templates - Criar template
- PUT /api/v1/admin/notification-templates/{id} - Atualizar template
- DELETE /api/v1/admin/notification-templates/{id} - Deletar template

## Integração com Eventos

### Subscription Events
- Ao criar subscription → SUBSCRIPTION_REQUESTED (para provider)
- Ao aprovar subscription → SUBSCRIPTION_APPROVED (para consumer)
- Ao revogar subscription → SUBSCRIPTION_REVOKED (para consumer)

### API Events
- Ao publicar nova versão → API_VERSION_RELEASED (para consumers subscritos)
- Ao depreciar API → API_DEPRECATED (para consumers subscritos)

### Gateway Events
- Ao atingir 80% do rate limit → RATE_LIMIT_WARNING
- Ao exceder rate limit → RATE_LIMIT_EXCEEDED

## Status
- [x] Análise técnica
- [x] Design do banco de dados
- [x] Implementação backend (entities, repositories, services)
- [x] Implementação de templates
- [x] Integração com email service
- [x] Implementação frontend (badge, dropdown, página)
- [x] Configurações de email no admin
- [ ] Testes

## Configuração de Email

### Para Administradores
1. Acesse **Configurações > Configurações de Email**
2. Configure o servidor SMTP:
   - Host: smtp.gmail.com (ou outro provedor)
   - Porta: 587 (TLS) ou 465 (SSL)
   - Usuário: seu-email@gmail.com
   - Senha: senha ou App Password (para Gmail)
3. Configure o remetente:
   - Email Remetente: noreply@apiportal.com
   - Nome do Remetente: API Portal
4. Habilite o envio de emails
5. Salve e reinicie o backend

### Gmail App Password
Para usar Gmail, você precisa criar um App Password:
1. Acesse https://myaccount.google.com/apppasswords
2. Crie uma nova senha de app
3. Use essa senha no campo "Senha" das configurações

### Variáveis de Ambiente (Alternativa)
Você também pode configurar via variáveis de ambiente:
```bash
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-app
MAIL_FROM_EMAIL=noreply@apiportal.com
MAIL_FROM_NAME=API Portal
```

## Como Testar

### 1. Testar Notificações In-App
1. Como Consumer, solicite uma subscription para uma API
2. Como Provider, veja a notificação no sino do header
3. Clique na notificação para ver detalhes
4. Aprove a subscription
5. Como Consumer, veja a notificação de aprovação

### 2. Testar Notificações por Email
1. Configure o email nas configurações (admin)
2. Reinicie o backend
3. Acesse **Configurações > Notificações**
4. Habilite notificações por email para os tipos desejados
5. Realize uma ação que gere notificação (ex: solicitar subscription)
6. Verifique seu email

### 3. Testar Preferências
1. Acesse **Configurações > Notificações**
2. Desabilite notificações por email para um tipo específico
3. Realize uma ação desse tipo
4. Verifique que recebeu apenas notificação in-app

## Implementado

### Backend
- ✅ Entidades: Notification, NotificationPreference, NotificationTemplate
- ✅ Repositories com queries otimizadas
- ✅ Services: NotificationService, NotificationTemplateService, EmailNotificationService
- ✅ Controller com 8 endpoints REST
- ✅ Sistema de eventos para subscriptions
- ✅ Migrations com tabelas e templates padrão
- ✅ Configuração de email via banco de dados
- ✅ JavaMailSender configurável

### Frontend
- ✅ NotificationService com polling automático
- ✅ Topbar com sino, badge e dropdown
- ✅ Página completa de notificações
- ✅ Seção de preferências no Settings
- ✅ Seção de configurações de email (admin)
- ✅ Rotas configuradas
- ✅ Ícones e estilos por tipo

## Próximos Passos
1. Implementar notificações para API_VERSION_RELEASED
2. Implementar notificações para RATE_LIMIT_WARNING/EXCEEDED
3. Adicionar testes unitários e de integração
4. Adicionar suporte a múltiplos idiomas nos templates
