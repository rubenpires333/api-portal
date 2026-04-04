# Correção de Notificações de Levantamento

## Problema Identificado

As notificações de levantamento (withdrawals) estão sendo enviadas por email, mas não aparecem:
1. No ícone de sino (bell) da plataforma
2. Nas Preferências de Notificações para Admin (só aparece API_APPROVAL_REQUESTED)
3. Nas Preferências de Notificações para Provider (não aparecem os tipos de transações de levantamento)

## Causa Raiz

1. Os templates de notificação podem não ter sido criados corretamente
2. As preferências de notificação não foram criadas para todos os usuários
3. O código estava criando notificações mas faltavam os métodos para aprovação e rejeição

## Solução Implementada

### 1. Backend - WithdrawalService.java

Adicionados métodos de notificação para:
- `notifyProviderAboutApproval()` - Notifica provider quando levantamento é aprovado
- `notifyProviderAboutRejection()` - Notifica provider quando levantamento é rejeitado
- Melhorado `notifyAdminsAboutWithdrawal()` - Inclui mais informações do provider

### 2. Frontend - notification.service.ts

Adicionados tipos de notificação faltantes:
- `API_APPROVAL_REQUESTED`
- `API_APPROVED`
- `API_REJECTED`
- `PAYMENT_REQUIRED`
- `PAYMENT_RECEIVED`

### 3. Frontend - settings.component.ts

Atualizados labels e descrições para incluir todos os tipos de notificação.

### 4. Scripts SQL

Criados dois scripts:
- `add_withdrawal_notification_templates.sql` - Cria templates e preferências
- `fix_withdrawal_notifications.sql` - Verifica e corrige problemas

## Como Aplicar a Correção

### Passo 1: Executar Script SQL

```bash
# Conectar ao PostgreSQL
psql -U apiportal -d db_portal_api

# Executar o script de correção
\i scripts/fix_withdrawal_notifications.sql

# Ou se preferir executar o script completo novamente
\i scripts/add_withdrawal_notification_templates.sql
```

### Passo 2: Reiniciar o Backend

```bash
# Parar a aplicação (Ctrl+C se estiver rodando)
# Recompilar e iniciar
mvn clean install
mvn spring-boot:run
```

### Passo 3: Limpar Cache do Frontend

```bash
cd ../frontend
npm run build
# Ou se estiver em desenvolvimento
ng serve --port 4200
```

### Passo 4: Testar

1. Fazer login como Admin
2. Ir em Configurações > Preferências de Notificações
3. Verificar se aparecem os tipos:
   - Solicitação de Levantamento (WITHDRAWAL_REQUESTED)
   - Levantamento Aprovado (WITHDRAWAL_APPROVED)
   - Levantamento Rejeitado (WITHDRAWAL_REJECTED)
   - Solicitação de Aprovação de API (API_APPROVAL_REQUESTED)
   - API Aprovada (API_APPROVED)
   - API Rejeitada (API_REJECTED)
   - Pagamento Necessário (PAYMENT_REQUIRED)
   - Pagamento Recebido (PAYMENT_RECEIVED)

4. Fazer login como Provider
5. Solicitar um levantamento
6. Verificar se o Admin recebe notificação no sino
7. Aprovar/Rejeitar o levantamento como Admin
8. Verificar se o Provider recebe notificação no sino

## Verificação Manual no Banco de Dados

```sql
-- Verificar templates criados
SELECT type, channel, subject 
FROM notification_templates 
WHERE type LIKE 'WITHDRAWAL%'
ORDER BY type, channel;

-- Verificar preferências de um usuário específico
SELECT notification_type, in_app_enabled, email_enabled
FROM notification_preferences
WHERE user_id = 'SEU_USER_ID'
AND notification_type LIKE 'WITHDRAWAL%';

-- Verificar notificações criadas
SELECT type, title, is_read, created_at
FROM notifications
WHERE type LIKE 'WITHDRAWAL%'
ORDER BY created_at DESC
LIMIT 10;

-- Contar preferências por tipo
SELECT notification_type, COUNT(*) as total
FROM notification_preferences
WHERE notification_type LIKE 'WITHDRAWAL%'
GROUP BY notification_type;
```

## Troubleshooting

### Problema: Preferências não aparecem no frontend

**Solução:**
```sql
-- Criar preferências manualmente para um usuário
INSERT INTO notification_preferences (id, user_id, notification_type, in_app_enabled, email_enabled, created_at, updated_at)
VALUES 
  (gen_random_uuid(), 'SEU_USER_ID', 'WITHDRAWAL_REQUESTED', true, true, NOW(), NOW()),
  (gen_random_uuid(), 'SEU_USER_ID', 'WITHDRAWAL_APPROVED', true, true, NOW(), NOW()),
  (gen_random_uuid(), 'SEU_USER_ID', 'WITHDRAWAL_REJECTED', true, true, NOW(), NOW())
ON CONFLICT (user_id, notification_type) DO NOTHING;
```

### Problema: Notificações não aparecem no sino

**Verificar:**
1. Se a preferência `in_app_enabled` está `true`
2. Se a notificação foi criada na tabela `notifications`
3. Se o `user_id` está correto
4. Se o frontend está fazendo polling corretamente

```sql
-- Verificar notificações de um usuário
SELECT * FROM notifications 
WHERE user_id = 'SEU_USER_ID' 
ORDER BY created_at DESC 
LIMIT 5;
```

### Problema: Email enviado mas notificação in-app não criada

**Verificar logs do backend:**
```bash
# Procurar por erros de notificação
grep -i "notification" logs/spring.log | grep -i "error"

# Verificar se o método sendNotification foi chamado
grep -i "sendNotification" logs/spring.log
```

## Arquivos Modificados

### Backend
- `WithdrawalService.java` - Adicionados métodos de notificação
- `NotificationType.java` - Já continha os tipos necessários

### Frontend
- `notification.service.ts` - Adicionados tipos faltantes
- `settings.component.ts` - Atualizados labels e descrições

### Scripts SQL
- `add_withdrawal_notification_templates.sql` - Templates e preferências
- `fix_withdrawal_notifications.sql` - Verificação e correção

## Próximos Passos

1. Executar o script SQL de correção
2. Reiniciar backend e frontend
3. Testar fluxo completo de levantamento
4. Verificar se todas as notificações aparecem corretamente
5. Confirmar que emails e notificações in-app funcionam

## Notas Importantes

- As notificações são criadas em uma transação separada (`REQUIRES_NEW`) para não afetar a transação principal
- Se a notificação falhar, o levantamento ainda será processado
- Os logs mostrarão qualquer erro de notificação sem quebrar o fluxo
- As preferências são criadas automaticamente quando um usuário recebe sua primeira notificação de cada tipo
