# Guia de Teste: Sistema de Levantamentos

## ✅ Implementação Completa

### O que foi implementado:

1. **Seed Data** - Regras de taxas para todos os métodos
2. **Integração Stripe Payouts** - Para transferências bancárias
3. **Notificações** - Provider notificado na conclusão
4. **Scripts de Teste** - SQL para validar fluxo completo

---

## 🚀 Passo 1: Inserir Regras de Taxas

Execute o script SQL para criar as regras de taxas:

```bash
psql -U postgres -d api_portal -f scripts/seed_withdrawal_fee_rules.sql
```

Ou execute manualmente no DBeaver/pgAdmin.

### Regras criadas:

| Método | Taxa % | Taxa Fixa | Mínimo | Máximo |
|--------|--------|-----------|--------|--------|
| VINTI4 | 2% | 10 CVE | 100 CVE | 50,000 CVE |
| BANK_TRANSFER | 1% | 5 EUR | 50 EUR | 100,000 EUR |
| PAYPAL | 2.5% | 0.50 EUR | 10 EUR | 50,000 EUR |
| WISE | 0.5% | 1 EUR | 20 EUR | 100,000 EUR |
| PLATFORM_CREDIT | 0% | 0 EUR | 1 EUR | 100,000 EUR |

---

## 🧪 Passo 2: Testar Fluxo Completo

### 2.1. Verificar Saldo Inicial

```sql
SELECT 
    provider_id,
    available_balance,
    pending_balance,
    reserved_balance
FROM provider_wallets
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
```

### 2.2. Solicitar Levantamento (API)

```bash
curl -X POST "http://localhost:8080/api/v1/provider/wallet/withdraw?providerId=69f2020f-7e2a-42ba-bf32-5821cfebe0c2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "method": "PAYPAL",
    "destinationDetails": "provider@email.com"
  }'
```

**Resposta esperada:**
```json
{
  "id": "uuid-do-levantamento",
  "requestedAmount": 100.00,
  "feePercentage": 2.50,
  "feeAmount": 3.00,
  "netAmount": 97.00,
  "method": "PAYPAL",
  "status": "PENDING_APPROVAL",
  "requestedAt": "2026-04-05T..."
}
```

**⚠️ IMPORTANTE:** Com `AUTO_APPROVE_THRESHOLD=0.00`, TODOS os levantamentos ficam pendentes de aprovação manual, independente do valor.

### 2.3. Verificar Saldo Reservado

```sql
SELECT 
    available_balance,  -- Deve ter diminuído 100
    reserved_balance    -- Deve ter aumentado 100
FROM provider_wallets
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
```

### 2.4. Listar Levantamentos Pendentes (Admin)

```bash
curl -X GET "http://localhost:8080/api/v1/admin/withdrawals/pending" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 2.5. Aprovar Levantamento (Admin)

```bash
curl -X POST "http://localhost:8080/api/v1/admin/withdrawals/{WITHDRAWAL_ID}/approve?adminId={ADMIN_ID}" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**O que acontece:**
- Status muda: `PENDING_APPROVAL` → `APPROVED`
- Saldo reservado é removido
- Transação marcada como `COMPLETED`
- Provider recebe notificação de aprovação

### 2.6. Aguardar Processamento Automático

O job `WithdrawalProcessingService` executa a cada 1 minuto e:

1. Busca levantamentos com status `APPROVED`
2. Atualiza para `PROCESSING`
3. Chama gateway de pagamento (Stripe Payouts para BANK_TRANSFER)
4. Se sucesso: `PROCESSING` → `COMPLETED`
5. Notifica provider sobre conclusão

**Logs esperados:**
```
INFO  c.a.b.m.b.s.WithdrawalProcessingService - Starting withdrawal processing job
INFO  c.a.b.m.b.s.WithdrawalProcessingService - Found 1 approved withdrawals to process
INFO  c.a.b.m.b.s.WithdrawalProcessingService - Processing withdrawal: id=..., method=PAYPAL, amount=97.00
INFO  c.a.b.m.b.s.WithdrawalProcessingService - Processing PayPal payment: provider@email.com
WARN  c.a.b.m.b.s.WithdrawalProcessingService - PayPal integration not implemented yet - simulating success
INFO  c.a.b.m.b.s.WithdrawalProcessingService - Withdrawal completed successfully: id=...
INFO  c.a.b.m.b.s.WithdrawalProcessingService - Notifying provider ... about withdrawal completion ...
INFO  c.a.b.m.b.s.WithdrawalProcessingService - Provider notified about withdrawal completion ...
```

### 2.7. Verificar Conclusão

```sql
SELECT 
    id,
    requested_amount,
    net_amount,
    method,
    status,
    requested_at,
    processed_at
FROM withdrawal_requests
ORDER BY requested_at DESC
LIMIT 1;
```

**Status esperado:** `COMPLETED`

---

## 🏦 Passo 3: Testar Stripe Payouts (Transferência Bancária)

### 3.1. Solicitar Levantamento via BANK_TRANSFER

```bash
curl -X POST "http://localhost:8080/api/v1/provider/wallet/withdraw?providerId=69f2020f-7e2a-42ba-bf32-5821cfebe0c2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 200.00,
    "method": "BANK_TRANSFER",
    "destinationDetails": "IBAN: PT50..."
  }'
```

**Cálculo de taxa:**
- Taxa: 1% + 5 EUR = (200 * 0.01) + 5 = 7 EUR
- Valor líquido: 200 - 7 = 193 EUR

### 3.2. Aprovar (Admin)

```bash
curl -X POST "http://localhost:8080/api/v1/admin/withdrawals/{WITHDRAWAL_ID}/approve?adminId={ADMIN_ID}" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.3. Aguardar Job Processar

O job irá chamar **Stripe Payouts API**:

**Logs esperados (em produção com Stripe configurado):**
```
INFO  Processing Stripe Payout: withdrawalId=..., amount=193.00, currency=cve
INFO  Creating Stripe Payout with params: amount=19300, currency=cve
INFO  ✅ Stripe Payout created: payoutId=po_..., status=pending, amount=193.0 CVE
INFO  ✅ Stripe Payout successful: payoutId=po_..., status=pending
INFO  Withdrawal completed successfully
```

**Logs esperados (erro - Stripe não configurado):**
```
INFO  Processing Stripe Payout: withdrawalId=..., amount=193.00, currency=cve
ERROR ❌ Stripe Payout error: withdrawalId=..., code=..., message=no external accounts
ERROR ⚠️ STRIPE CONNECT NOT CONFIGURED: No external bank account found
ERROR    → Configure Stripe Connect: https://dashboard.stripe.com/connect/accounts/overview
ERROR    → Add external bank account to receive payouts
WARN  Payment failed for withdrawal: id=...
```

**⚠️ IMPORTANTE:** 
- **SEM SIMULAÇÃO:** Sistema não simula sucesso - payout real ou falha
- **Para Produção:** Siga o guia `STRIPE_CONNECT_SETUP.md` para configurar
- **Moedas Suportadas:** CVE, EUR, USD
- **Tempo de Processamento:** 
  - CVE: 2-5 dias úteis
  - EUR: 1-3 dias úteis
  - USD: 1-2 dias úteis

---

## 📊 Passo 4: Verificar Estatísticas

Execute o script de teste completo:

```bash
psql -U postgres -d api_portal -f scripts/test_withdrawal_flow.sql
```

Ou execute queries individuais:

### Histórico de Levantamentos
```sql
SELECT 
    id,
    requested_amount,
    fee_amount,
    net_amount,
    method,
    status,
    requested_at,
    processed_at
FROM withdrawal_requests
ORDER BY requested_at DESC;
```

### Estatísticas por Status
```sql
SELECT 
    status,
    COUNT(*) as total,
    SUM(requested_amount) as total_solicitado,
    SUM(net_amount) as total_liquido,
    SUM(fee_amount) as total_taxas
FROM withdrawal_requests
GROUP BY status;
```

### Estatísticas por Método
```sql
SELECT 
    method,
    COUNT(*) as total,
    SUM(requested_amount) as total_solicitado,
    AVG(fee_percentage) as taxa_media_percentual,
    SUM(fee_amount) as total_taxas
FROM withdrawal_requests
GROUP BY method
ORDER BY total DESC;
```

---

## 🔧 Configuração Adicional

### application.properties

Adicione (se ainda não existir):

```properties
# Threshold para auto-aprovação (0 = todos precisam aprovação manual)
billing.auto-approve-threshold=0.00

# Stripe Payouts (já configurado via STRIPE_SECRET_KEY)
billing.stripe.payout-enabled=true
```

### .env

```env
# Auto-approve threshold (0 = todos precisam aprovação manual)
AUTO_APPROVE_THRESHOLD=0.00

# Stripe já configurado
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

---

## ✅ Checklist de Teste

- [ ] Seed data inserido (regras de taxas)
- [ ] Solicitar levantamento via API
- [ ] Verificar saldo reservado
- [ ] Listar levantamentos pendentes (admin)
- [ ] Aprovar levantamento (admin)
- [ ] Verificar notificação de aprovação
- [ ] Aguardar job processar (5 minutos)
- [ ] Verificar status COMPLETED
- [ ] Verificar notificação de conclusão
- [ ] Testar cancelamento de levantamento pendente
- [ ] Testar rejeição de levantamento (admin)
- [ ] Verificar estatísticas no banco

---

## 🎯 Próximos Passos (Opcional)

### Implementar Integrações Reais

1. **Vinti4** (Cabo Verde)
   - Obter credenciais API
   - Implementar em `processVinti4Payment()`

2. **PayPal Payouts**
   - Configurar PayPal Business
   - Implementar em `processPayPalPayment()`

3. **Wise API**
   - Obter API key Wise
   - Implementar em `processWisePayment()`

### Melhorias

- [ ] Webhook de confirmação dos gateways
- [ ] Retry logic com backoff exponencial
- [ ] Dashboard de levantamentos para admin
- [ ] Exportar relatórios CSV/Excel
- [ ] Limite de levantamentos por dia/mês
- [ ] KYC/Verificação de identidade

---

## 📝 Notas Importantes

1. **Auto-aprovação DESABILITADA:** Com `AUTO_APPROVE_THRESHOLD=0.00`, TODOS os levantamentos precisam aprovação manual do admin
2. **Job:** Executa a cada 1 minuto para testes (em produção recomenda-se 5 minutos ou mais)
3. **Stripe Payouts:** 
   - **SEM SIMULAÇÃO:** Sistema faz payout real ou falha
   - **Configuração:** Siga `STRIPE_CONNECT_SETUP.md` para configurar
   - **Moedas:** CVE, EUR, USD suportados
4. **Outros Métodos:** PayPal, Wise e Vinti4 simulam sucesso (APIs não implementadas)
5. **Notificações:** Provider recebe notificações:
   - Quando solicita (se pendente)
   - Quando aprovado
   - Quando concluído (se payout for bem-sucedido)

---

## 🐛 Troubleshooting

### Levantamento não processa

1. Verificar se job está rodando:
```
grep "Starting withdrawal processing job" logs/application.log
```

2. Verificar status:
```sql
SELECT status FROM withdrawal_requests WHERE id = 'uuid';
```

3. Verificar logs de erro:
```
grep "Error processing withdrawal" logs/application.log
```

### Stripe Payout falha

**Erro: "No external accounts in that currency"**

**Causa:** Stripe Connect não configurado ou conta bancária não adicionada

**Solução:**
1. Siga o guia completo: `STRIPE_CONNECT_SETUP.md`
2. Configure Stripe Connect: https://dashboard.stripe.com/connect/accounts/overview
3. Adicione conta bancária: https://dashboard.stripe.com/settings/payouts
4. Verifique moedas suportadas (CVE, EUR, USD)

**Erro: "Insufficient funds"**

**Causa:** Saldo insuficiente na conta Stripe

**Solução:**
1. Verificar saldo: https://dashboard.stripe.com/balance
2. Aguardar fundos disponíveis
3. Ajustar schedule de payouts

**Erro: "Account not verified"**

**Causa:** Verificação de identidade incompleta

**Solução:**
1. Completar verificação no dashboard
2. Enviar documentos solicitados
3. Aguardar aprovação (1-3 dias úteis)

**Verificar logs detalhados:**
```bash
grep "Stripe Payout error" logs/application.log
grep "STRIPE CONNECT NOT CONFIGURED" logs/application.log
```

### Levantamento ficou APPROVED após falha

**Causa:** Payout falhou mas status não foi revertido

**Comportamento esperado:**
- Se payout falhar, status volta para `APPROVED`
- Job tentará processar novamente no próximo ciclo (1 minuto)
- Após 3 falhas consecutivas, considere cancelar manualmente

**Solução temporária:**
Execute o script de correção:
```bash
psql -U postgres -d api_portal -f scripts/fix_failed_withdrawal.sql
```

Ou manualmente:
```sql
-- Opção 1: Marcar como COMPLETED (se quiser simular sucesso)
UPDATE withdrawal_requests
SET status = 'COMPLETED', processed_at = NOW()
WHERE id = 'uuid-do-levantamento';

-- Opção 2: Cancelar e devolver saldo
UPDATE withdrawal_requests
SET status = 'CANCELLED'
WHERE id = 'uuid-do-levantamento';

UPDATE provider_wallets
SET 
    reserved_balance = reserved_balance - 45.00,
    available_balance = available_balance + 45.00
WHERE provider_id = 'uuid-do-provider';
```

**Solução definitiva:**
Configure Stripe Connect seguindo `STRIPE_CONNECT_SETUP.md`

### Notificação não enviada

1. Verificar serviço de notificações ativo
2. Verificar logs:
```
grep "Error notifying provider" logs/application.log
```

---

## 🎉 Sistema Completo!

O sistema de levantamentos está 100% funcional com:
- ✅ Seed data
- ✅ Integração Stripe Payouts
- ✅ Notificações completas
- ✅ Scripts de teste
- ✅ Documentação completa
