# Teste Rápido: Stripe Payout com EUR

## ✅ Pré-requisitos Completos

- [x] Conta Stripe configurada
- [x] Conta bancária EUR adicionada
- [x] Sistema rodando
- [x] Provider com saldo disponível

---

## 🚀 Teste em 5 Passos

### 1️⃣ Atualizar Moeda da Carteira para EUR

```bash
psql -U postgres -d api_portal -f scripts/update_wallet_currency_eur.sql
```

**Resultado esperado:**
```
currency | available_balance | reserved_balance
---------+-------------------+-----------------
EUR      | 100.00            | 0.00
```

---

### 2️⃣ Solicitar Levantamento (Provider)

```bash
curl -X POST "http://localhost:8080/api/v1/provider/wallet/withdraw?providerId=69f2020f-7e2a-42ba-bf32-5821cfebe0c2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "method": "BANK_TRANSFER",
    "destinationDetails": "IBAN: PT50..."
  }'
```

**Resposta esperada:**
```json
{
  "id": "uuid-do-levantamento",
  "requestedAmount": 100.00,
  "feePercentage": 1.00,
  "feeAmount": 6.00,
  "netAmount": 94.00,
  "method": "BANK_TRANSFER",
  "status": "PENDING_APPROVAL",
  "requestedAt": "2026-04-05T..."
}
```

**Cálculo da taxa:**
- Taxa: 1% + 5 EUR = (100 × 0.01) + 5 = 6 EUR
- Valor líquido: 100 - 6 = 94 EUR

---

### 3️⃣ Aprovar Levantamento (Admin)

```bash
# Listar pendentes
curl -X GET "http://localhost:8080/api/v1/admin/withdrawals/pending" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Aprovar
curl -X POST "http://localhost:8080/api/v1/admin/withdrawals/{WITHDRAWAL_ID}/approve?adminId={ADMIN_ID}" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Resposta esperada:**
```
HTTP 200 OK
```

---

### 4️⃣ Aguardar Processamento (1 minuto)

O job `WithdrawalProcessingService` executa automaticamente a cada 1 minuto.

**Monitorar logs:**
```bash
tail -f logs/application.log | grep "Stripe Payout"
```

**Logs esperados (SUCESSO):**
```
INFO  Processing Stripe Payout: withdrawalId=..., amount=94.00, currency=eur
INFO  Creating Stripe Payout with params: amount=9400, currency=eur
INFO  ✅ Stripe Payout created: payoutId=po_1ABC123..., status=pending, amount=94.0 EUR
INFO  ✅ Stripe Payout successful: payoutId=po_1ABC123..., status=pending
INFO  Withdrawal completed successfully: id=...
INFO  Notifying provider ... about withdrawal completion ...
```

**Logs esperados (ERRO - se algo falhar):**
```
ERROR ❌ Stripe Payout error: withdrawalId=..., code=..., message=...
ERROR ⚠️ STRIPE CONNECT NOT CONFIGURED: No external bank account found
WARN  Payment failed for withdrawal: id=...
```

---

### 5️⃣ Verificar Resultado

**No Banco de Dados:**
```bash
psql -U postgres -d api_portal -f scripts/test_stripe_payout_eur.sql
```

**Status esperado:**
```
id                  | status    | net_amount | processed_at
--------------------+-----------+------------+-------------
uuid-do-levantamento| COMPLETED | 94.00      | 2026-04-05...
```

**No Stripe Dashboard:**
1. Acesse: https://dashboard.stripe.com/test/payouts (teste)
2. Ou: https://dashboard.stripe.com/payouts (produção)
3. Procure pelo payout ID: `po_1ABC123...`

**Detalhes do Payout:**
- Amount: 94.00 EUR
- Status: Pending → Paid (2-3 dias úteis)
- Destination: Sua conta bancária EUR
- Metadata: withdrawalId, providerId, etc.

---

## 📊 Verificações Finais

### Saldo da Carteira

```sql
SELECT 
    currency,
    available_balance,  -- Deve ter diminuído 100
    reserved_balance,   -- Deve estar em 0
    pending_balance
FROM provider_wallets
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
```

**Esperado:**
- `available_balance`: Saldo inicial - 100 EUR
- `reserved_balance`: 0.00 EUR
- `pending_balance`: Inalterado

### Status do Levantamento

```sql
SELECT id, status, net_amount, processed_at
FROM withdrawal_requests
ORDER BY requested_at DESC
LIMIT 1;
```

**Esperado:**
- `status`: COMPLETED
- `net_amount`: 94.00
- `processed_at`: Data/hora atual

### Transação na Carteira

```sql
SELECT amount, type, status, description
FROM wallet_transactions
WHERE type = 'DEBIT_WITHDRAWAL'
ORDER BY created_at DESC
LIMIT 1;
```

**Esperado:**
- `amount`: -100.00 (negativo = débito)
- `type`: DEBIT_WITHDRAWAL
- `status`: COMPLETED
- `description`: "Solicitação de levantamento via BANK_TRANSFER"

---

## 🎉 Sucesso!

Se todos os passos funcionaram:

✅ Levantamento solicitado  
✅ Admin aprovou  
✅ Job processou automaticamente  
✅ Stripe Payout criado  
✅ Status marcado como COMPLETED  
✅ Saldo debitado corretamente  
✅ Provider notificado  
✅ Payout visível no Stripe Dashboard  

**Próximos passos:**
1. Aguardar 2-3 dias úteis para payout chegar na conta bancária
2. Confirmar recebimento
3. Testar com outros valores
4. Configurar para produção (chaves live)

---

## 🐛 Troubleshooting

### Erro: "No external accounts in that currency"

**Solução:**
1. Verificar conta EUR adicionada: https://dashboard.stripe.com/settings/payouts
2. Verificar moeda da carteira: `SELECT currency FROM provider_wallets`
3. Adicionar conta bancária EUR se necessário

### Levantamento ficou APPROVED

**Solução:**
1. Verificar logs: `grep "Stripe Payout error" logs/application.log`
2. Corrigir erro (conta bancária, saldo, etc.)
3. Aguardar próximo ciclo do job (1 minuto)
4. Ou executar: `scripts/fix_failed_withdrawal.sql`

### Payout não aparece no Stripe

**Solução:**
1. Verificar chave API correta (test vs live)
2. Verificar logs: `grep "Stripe Payout created" logs/application.log`
3. Procurar por payoutId: `po_...`
4. Verificar filtros no dashboard (data, status)

---

## 📞 Suporte

- Logs: `logs/application.log`
- Stripe Dashboard: https://dashboard.stripe.com
- Stripe Support: https://support.stripe.com
- Documentação: `STRIPE_CONNECT_SETUP.md`

---

## ✅ Checklist de Teste

- [ ] Moeda da carteira atualizada para EUR
- [ ] Levantamento solicitado via API
- [ ] Status inicial: PENDING_APPROVAL
- [ ] Admin aprovou levantamento
- [ ] Job processou em 1 minuto
- [ ] Logs mostram "Stripe Payout created"
- [ ] Status final: COMPLETED
- [ ] Saldo debitado corretamente
- [ ] Payout visível no Stripe Dashboard
- [ ] Provider recebeu notificação

**Sistema funcionando 100%!** 🚀
