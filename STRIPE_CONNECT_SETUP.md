# Configuração Stripe Connect para Levantamentos

## 🎯 Objetivo

Configurar Stripe Connect para permitir que providers recebam levantamentos (payouts) diretamente em suas contas bancárias.

---

## 📋 Pré-requisitos

1. Conta Stripe ativa
2. Verificação de identidade completa
3. Conta bancária ou cartão de débito para receber fundos

---

## 🔧 Passo 1: Ativar Stripe Connect

### 1.1. Acessar Dashboard

1. Acesse: https://dashboard.stripe.com/connect/accounts/overview
2. Clique em "Get started with Connect"

### 1.2. Escolher Tipo de Integração

Para este sistema, use **Standard Connect**:
- Providers criam suas próprias contas Stripe
- Você (plataforma) não precisa gerenciar KYC
- Providers recebem payouts diretamente

---

## 🏦 Passo 2: Adicionar Conta Externa (Bank Account)

### 2.1. Para Cabo Verde (CVE)

**Opção A: Conta Bancária Local**
```
Banco: Banco Comercial do Atlântico (BCA), Caixa Económica, etc.
IBAN: CV64...
SWIFT/BIC: BCAVXXXX
Moeda: CVE
```

**Opção B: Conta USD (Alternativa)**
```
Se CVE não for suportado, use conta USD:
- Banco internacional (ex: Wise, Revolut)
- IBAN/Account Number
- SWIFT/BIC
- Moeda: USD
```

### 2.2. Adicionar no Dashboard

1. Acesse: https://dashboard.stripe.com/settings/payouts
2. Clique em "Add bank account"
3. Preencha:
   - País: Cape Verde (CV)
   - Moeda: CVE ou USD
   - IBAN ou Account Number
   - Nome do titular
   - SWIFT/BIC (se necessário)

4. Verificar conta:
   - Stripe fará 2 micro-depósitos
   - Confirme os valores no dashboard

---

## 💰 Passo 3: Configurar Moedas Suportadas

### 3.1. Verificar Moedas Disponíveis

Stripe Payouts suporta:
- ✅ **CVE** (Escudo Cabo-Verdiano) - Suportado
- ✅ **EUR** (Euro) - Suportado
- ✅ **USD** (Dólar Americano) - Suportado

### 3.2. Configurar no Sistema

No arquivo `.env`:

```env
# Moeda padrão da plataforma
PLATFORM_CURRENCY=CVE

# Moedas suportadas (separadas por vírgula)
SUPPORTED_CURRENCIES=CVE,EUR,USD
```

### 3.3. Atualizar Carteiras

```sql
-- Verificar moeda atual das carteiras
SELECT provider_id, currency FROM provider_wallets;

-- Atualizar para CVE (se necessário)
UPDATE provider_wallets SET currency = 'CVE';

-- Ou manter EUR/USD se preferir
UPDATE provider_wallets SET currency = 'EUR';
```

---

## 🔐 Passo 4: Configurar Webhooks (Opcional)

Para receber confirmações de payouts:

### 4.1. Criar Webhook Endpoint

1. Acesse: https://dashboard.stripe.com/webhooks
2. Clique em "Add endpoint"
3. URL: `https://seu-dominio.com/api/v1/billing/webhook/stripe`
4. Eventos a escutar:
   - `payout.paid` - Payout concluído
   - `payout.failed` - Payout falhou
   - `payout.canceled` - Payout cancelado

### 4.2. Implementar Handler (Futuro)

```java
@PostMapping("/webhook/stripe/payout")
public ResponseEntity<String> handlePayoutWebhook(@RequestBody String payload) {
    // Verificar assinatura
    // Processar evento
    // Atualizar status do levantamento
}
```

---

## 💳 Passo 5: Testar Payouts

### 5.1. Ambiente de Teste

Use chaves de teste do Stripe:

```env
STRIPE_API_KEY=sk_test_...
```

**Contas bancárias de teste:**
```
IBAN: CV64000000000000000000000
Account Number: 000123456789
Routing Number: 110000000
```

### 5.2. Fazer Payout de Teste

```bash
# 1. Solicitar levantamento
curl -X POST "http://localhost:8080/api/v1/provider/wallet/withdraw?providerId=..." \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "amount": 100.00,
    "method": "BANK_TRANSFER",
    "destinationDetails": "IBAN: CV64..."
  }'

# 2. Aprovar (admin)
curl -X POST "http://localhost:8080/api/v1/admin/withdrawals/{ID}/approve?adminId=..."

# 3. Aguardar job processar (1 minuto)

# 4. Verificar no Stripe Dashboard
# https://dashboard.stripe.com/test/payouts
```

### 5.3. Verificar Logs

```bash
grep "Stripe Payout" logs/application.log
```

**Logs esperados (sucesso):**
```
INFO  Processing Stripe Payout: withdrawalId=..., amount=100.00, currency=cve
INFO  Creating Stripe Payout with params: amount=10000, currency=cve
INFO  ✅ Stripe Payout created: payoutId=po_..., status=pending, amount=100.0 CVE
INFO  ✅ Stripe Payout successful: payoutId=po_..., status=pending
```

**Logs esperados (erro):**
```
ERROR ❌ Stripe Payout error: withdrawalId=..., code=..., message=...
ERROR ⚠️ STRIPE CONNECT NOT CONFIGURED: No external bank account found
ERROR    → Configure Stripe Connect: https://dashboard.stripe.com/connect/accounts/overview
```

---

## 🌍 Passo 6: Produção

### 6.1. Ativar Modo Produção

1. Complete verificação de identidade no Stripe
2. Adicione conta bancária real
3. Atualize chaves para produção:

```env
STRIPE_API_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### 6.2. Configurar Limites

```env
# Valor mínimo de levantamento (CVE)
MINIMUM_WITHDRAWAL_AMOUNT=100.00

# Valor máximo de levantamento (CVE)
MAXIMUM_WITHDRAWAL_AMOUNT=50000.00

# Threshold de auto-aprovação (0 = todos precisam aprovação)
AUTO_APPROVE_THRESHOLD=0.00
```

### 6.3. Configurar Taxas

```sql
-- Atualizar taxas de levantamento para CVE
UPDATE withdrawal_fee_rules
SET 
    fee_percentage = 2.00,
    fixed_fee = 10.00,
    fixed_fee_currency = 'CVE',
    minimum_amount = 100.00,
    maximum_amount = 50000.00
WHERE withdrawal_method = 'BANK_TRANSFER';
```

---

## 📊 Monitoramento

### Dashboard Stripe

Monitore payouts em:
- https://dashboard.stripe.com/payouts (produção)
- https://dashboard.stripe.com/test/payouts (teste)

### Métricas Importantes

1. **Taxa de sucesso:** % de payouts concluídos
2. **Tempo médio:** Tempo até payout ser pago
3. **Falhas:** Motivos de falha mais comuns
4. **Volume:** Total processado por dia/mês

### Queries SQL

```sql
-- Estatísticas de levantamentos
SELECT 
    status,
    COUNT(*) as total,
    SUM(requested_amount) as total_amount,
    AVG(requested_amount) as avg_amount
FROM withdrawal_requests
WHERE method = 'BANK_TRANSFER'
GROUP BY status;

-- Levantamentos por dia
SELECT 
    DATE(requested_at) as date,
    COUNT(*) as total,
    SUM(requested_amount) as total_amount
FROM withdrawal_requests
WHERE method = 'BANK_TRANSFER'
GROUP BY DATE(requested_at)
ORDER BY date DESC;
```

---

## 🚨 Troubleshooting

### Erro: "No external accounts in that currency"

**Causa:** Conta bancária não configurada ou moeda não suportada

**Solução:**
1. Adicionar conta bancária no dashboard
2. Verificar se moeda é suportada
3. Considerar usar USD como alternativa

### Erro: "Insufficient funds"

**Causa:** Saldo insuficiente na conta Stripe

**Solução:**
1. Verificar saldo: https://dashboard.stripe.com/balance
2. Aguardar fundos disponíveis
3. Ajustar schedule de payouts

### Erro: "Account not verified"

**Causa:** Verificação de identidade incompleta

**Solução:**
1. Completar verificação no dashboard
2. Enviar documentos solicitados
3. Aguardar aprovação (1-3 dias úteis)

### Payout demora muito

**Causa:** Tempo de processamento bancário

**Solução:**
- CVE: 2-5 dias úteis
- EUR: 1-3 dias úteis
- USD: 1-2 dias úteis

---

## 📚 Recursos

### Documentação Oficial

- Stripe Payouts: https://stripe.com/docs/payouts
- Stripe Connect: https://stripe.com/docs/connect
- Moedas suportadas: https://stripe.com/docs/payouts#supported-currencies
- API Reference: https://stripe.com/docs/api/payouts

### Suporte

- Stripe Support: https://support.stripe.com
- Status: https://status.stripe.com
- Community: https://stripe.com/community

---

## ✅ Checklist de Produção

- [ ] Conta Stripe verificada
- [ ] Stripe Connect ativado
- [ ] Conta bancária adicionada e verificada
- [ ] Moeda configurada (CVE/EUR/USD)
- [ ] Chaves de produção configuradas
- [ ] Webhooks configurados (opcional)
- [ ] Limites e taxas configurados
- [ ] Teste de payout realizado
- [ ] Monitoramento configurado
- [ ] Documentação para providers criada

---

## 🎉 Pronto para Produção!

Após completar todos os passos, o sistema estará pronto para processar levantamentos reais via Stripe Payouts.

**Próximos passos:**
1. Testar com valor pequeno (ex: 10 CVE)
2. Verificar recebimento na conta bancária
3. Documentar processo para providers
4. Monitorar primeiros levantamentos
5. Ajustar taxas e limites conforme necessário
