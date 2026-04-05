# Limites de Payout do Stripe

## ⚠️ Valores Mínimos por Moeda

O Stripe impõe valores mínimos para payouts que variam por moeda:

| Moeda | Mínimo | Equivalente |
|-------|--------|-------------|
| **EUR** | 200.00 EUR | ~200 EUR |
| **USD** | 1.00 USD | ~1 USD |
| **GBP** | 1.00 GBP | ~1 GBP |
| **CVE** | ~20,000 CVE | ~200 EUR |

**Fonte:** https://stripe.com/docs/payouts#minimum-payout-amounts

---

## 🚨 Problema Identificado

### Erro Atual
```
ERROR ❌ Stripe Payout error: code=amount_too_small
message=Amount must be no less than €200.00
```

### Causa
Tentativa de fazer payout de **53.90 EUR**, mas Stripe requer **mínimo de 200 EUR**.

---

## ✅ Solução Implementada

### 1. Validação Antes do Payout

O sistema agora valida o valor mínimo ANTES de chamar a API do Stripe:

```java
private java.math.BigDecimal getStripeMinimumAmount(String currency) {
    switch (currency.toLowerCase()) {
        case "eur": return BigDecimal.valueOf(200.00);
        case "usd": return BigDecimal.valueOf(1.00);
        case "cve": return BigDecimal.valueOf(20000.00);
        default: return BigDecimal.valueOf(1.00);
    }
}
```

### 2. Logs Informativos

Quando valor é menor que o mínimo:
```
ERROR ❌ Amount too small for Stripe Payout: 53.90 EUR (minimum: 200.00 EUR)
ERROR ⚠️ STRIPE MINIMUM NOT MET: Withdrawal amount must be at least 200.00 EUR
ERROR    → Consider using alternative payment method (PayPal, Wise, Vinti4)
ERROR    → Or accumulate balance until minimum is reached
```

### 3. Regras de Taxa Atualizadas

```sql
UPDATE withdrawal_fee_rules
SET minimum_amount = 200.00
WHERE withdrawal_method = 'BANK_TRANSFER';
```

---

## 💡 Alternativas para Valores Menores

### Opção 1: Acumular Saldo

Provider aguarda até ter pelo menos 200 EUR disponível:
- Saldo atual: 53.90 EUR
- Necessário: 200.00 EUR
- Faltam: 146.10 EUR

**Vantagens:**
- Usa Stripe (mais confiável)
- Taxa menor (1% + 5 EUR)

**Desvantagens:**
- Provider precisa esperar

### Opção 2: Usar PayPal

PayPal tem mínimo muito menor (10 EUR):
- Mínimo: 10 EUR ✅
- Taxa: 2.5% + 0.50 EUR
- Processamento: 1-2 dias úteis

**Exemplo:**
- Valor: 53.90 EUR
- Taxa: (53.90 × 0.025) + 0.50 = 1.85 EUR
- Líquido: 52.05 EUR

### Opção 3: Usar Wise

Wise tem mínimo de 20 EUR:
- Mínimo: 20 EUR ✅
- Taxa: 0.5% + 1 EUR (mais barato!)
- Processamento: 1-3 dias úteis

**Exemplo:**
- Valor: 53.90 EUR
- Taxa: (53.90 × 0.005) + 1.00 = 1.27 EUR
- Líquido: 52.63 EUR

### Opção 4: Usar Vinti4 (Cabo Verde)

Para providers em Cabo Verde:
- Mínimo: 100 CVE (~0.91 EUR) ✅
- Taxa: 2% + 10 CVE
- Processamento: Instantâneo

---

## 🔧 Configuração Recomendada

### Para Produção

```sql
-- Stripe (Bank Transfer): Valores altos
UPDATE withdrawal_fee_rules
SET 
    minimum_amount = 200.00,
    maximum_amount = 100000.00
WHERE withdrawal_method = 'BANK_TRANSFER';

-- PayPal: Valores médios
UPDATE withdrawal_fee_rules
SET 
    minimum_amount = 10.00,
    maximum_amount = 50000.00
WHERE withdrawal_method = 'PAYPAL';

-- Wise: Valores pequenos/médios (melhor taxa)
UPDATE withdrawal_fee_rules
SET 
    minimum_amount = 20.00,
    maximum_amount = 100000.00
WHERE withdrawal_method = 'WISE';

-- Vinti4: Valores pequenos (Cabo Verde)
UPDATE withdrawal_fee_rules
SET 
    minimum_amount = 100.00,  -- CVE
    maximum_amount = 50000.00
WHERE withdrawal_method = 'VINTI4';
```

### Mensagem para Providers

```
⚠️ Levantamento via Transferência Bancária (Stripe)

Valor mínimo: 200 EUR
Seu saldo: 53.90 EUR

Opções:
1. Aguardar até ter 200 EUR (faltam 146.10 EUR)
2. Usar PayPal (mínimo: 10 EUR)
3. Usar Wise (mínimo: 20 EUR, taxa menor)
4. Usar Vinti4 (mínimo: 1 CVE, apenas Cabo Verde)
```

---

## 📊 Comparação de Métodos

| Método | Mínimo | Taxa | Tempo | Melhor Para |
|--------|--------|------|-------|-------------|
| **Stripe** | 200 EUR | 1% + 5 EUR | 2-3 dias | Valores altos (>200 EUR) |
| **Wise** | 20 EUR | 0.5% + 1 EUR | 1-3 dias | Valores médios (20-200 EUR) |
| **PayPal** | 10 EUR | 2.5% + 0.50 EUR | 1-2 dias | Valores pequenos (10-50 EUR) |
| **Vinti4** | 1 CVE | 2% + 10 CVE | Instantâneo | Cabo Verde (qualquer valor) |

---

## 🧪 Testar com Valor Correto

### Teste 1: Valor Mínimo (200 EUR)

```bash
curl -X POST "http://localhost:8080/api/v1/provider/wallet/withdraw?providerId=..." \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "amount": 200.00,
    "method": "BANK_TRANSFER",
    "destinationDetails": "IBAN: PT50..."
  }'
```

**Cálculo:**
- Valor: 200.00 EUR
- Taxa: (200 × 0.01) + 5 = 7.00 EUR
- Líquido: 193.00 EUR ✅ (acima do mínimo)

### Teste 2: Valor Maior (500 EUR)

```bash
curl -X POST "..." \
  -d '{
    "amount": 500.00,
    "method": "BANK_TRANSFER",
    "destinationDetails": "IBAN: PT50..."
  }'
```

**Cálculo:**
- Valor: 500.00 EUR
- Taxa: (500 × 0.01) + 5 = 10.00 EUR
- Líquido: 490.00 EUR ✅

---

## 📝 Atualizar Sistema

### 1. Atualizar Regras de Taxa

```bash
psql -U postgres -d api_portal -f scripts/update_bank_transfer_minimum.sql
```

### 2. Reiniciar Aplicação

Para carregar nova validação de mínimo.

### 3. Testar com Valor Correto

```bash
# Solicitar 200 EUR
curl -X POST "..." -d '{"amount": 200.00, ...}'

# Aprovar
curl -X POST ".../approve?adminId=..."

# Aguardar 1 minuto (job processa)

# Verificar logs
tail -f logs/application.log | grep "Stripe Payout"
```

**Logs esperados:**
```
INFO  Processing Stripe Payout: amount=193.00, currency=eur
INFO  ✅ Stripe Payout created: payoutId=po_..., status=pending, amount=193.0 EUR
INFO  ✅ Stripe Payout successful
```

---

## ✅ Checklist

- [ ] Atualizar mínimo de BANK_TRANSFER para 200 EUR
- [ ] Reiniciar aplicação
- [ ] Cancelar levantamento de 53.90 EUR (abaixo do mínimo)
- [ ] Devolver saldo para provider
- [ ] Testar com 200 EUR ou mais
- [ ] Verificar payout no Stripe Dashboard
- [ ] Documentar para providers sobre mínimos
- [ ] Considerar implementar PayPal/Wise para valores menores

---

## 🎯 Próximos Passos

1. **Curto Prazo:** Usar Stripe apenas para valores >= 200 EUR
2. **Médio Prazo:** Implementar Wise (melhor para 20-200 EUR)
3. **Longo Prazo:** Implementar PayPal e Vinti4 para cobertura completa

**Sistema agora valida mínimos corretamente!** ✅
