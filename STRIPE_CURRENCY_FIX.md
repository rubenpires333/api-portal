# 🔧 Correção: Stripe Currency Error

## ❌ Problema

```
Amount must convert to at least 50 cents. 
49.00 $ converts to approximately €0.44.
code: amount_too_small
```

### Causa
- Planos configurados com **CVE (Escudos Cabo-Verdianos)**
- CVE não é suportado diretamente pelo Stripe
- Stripe tenta converter CVE → EUR
- 49 CVE = 0,44 EUR (menor que mínimo de 0,50 EUR)

### Moedas Suportadas pelo Stripe
O Stripe suporta 135+ moedas, mas **CVE não está na lista**.

Moedas comuns suportadas:
- ✅ EUR (Euro)
- ✅ USD (Dólar Americano)
- ✅ GBP (Libra Esterlina)
- ✅ BRL (Real Brasileiro)
- ❌ CVE (Escudo Cabo-Verdiano) - NÃO SUPORTADO

---

## ✅ Solução

### Opção 1: Usar EUR (Recomendado)
EUR é a moeda mais comum na Europa e África.

**Valores sugeridos:**
- STARTER: 0,00 EUR (Free)
- GROWTH: 29,99 EUR/mês
- BUSINESS: 99,99 EUR/mês

### Opção 2: Usar USD
USD é aceito globalmente.

**Valores sugeridos:**
- STARTER: 0,00 USD (Free)
- GROWTH: 29,99 USD/mês
- BUSINESS: 99,99 USD/mês

---

## 🔧 Como Corrigir

### Passo 1: Executar Script SQL

```bash
# No MySQL
mysql -u root -p api_portal < scripts/fix_plan_currency.sql
```

Ou execute manualmente:

```sql
UPDATE platform_plans
SET 
    currency = 'EUR',
    monthly_price = CASE 
        WHEN name = 'STARTER' THEN 0.00
        WHEN name = 'GROWTH' THEN 29.99
        WHEN name = 'BUSINESS' THEN 99.99
        ELSE monthly_price
    END,
    updated_at = NOW()
WHERE currency = 'CVE' OR currency = 'USD';
```

### Passo 2: Verificar Planos

```sql
SELECT name, display_name, monthly_price, currency
FROM platform_plans
ORDER BY display_order;
```

**Resultado esperado:**
```
STARTER  | Starter  | 0.00  | EUR
GROWTH   | Growth   | 29.99 | EUR
BUSINESS | Business | 99.99 | EUR
```

### Passo 3: Reiniciar Aplicação (Opcional)

```bash
# Se houver cache
cd api-portal-backend
./mvnw spring-boot:run
```

---

## 🧪 Testar Novamente

### 1. Criar Nova Subscription

```bash
# Como Provider
POST /api/v1/billing/checkout/platform-payment-intent
{
  "planId": "{growth_plan_id}",
  "paymentMethod": "STRIPE"
}
```

### 2. Verificar Logs

Deve mostrar:
```
Creating Stripe Payment Intent: amount=29.99, currency=EUR
```

### 3. Completar Pagamento

- Usar cartão teste: `4242 4242 4242 4242`
- Valor: 29,99 EUR
- Pagamento deve ser aceito ✅

---

## 📊 Valores Mínimos do Stripe

### Por Moeda

| Moeda | Mínimo | Exemplo |
|-------|--------|---------|
| EUR   | 0,50 € | ✅ 29,99 € |
| USD   | $0.50  | ✅ $29.99 |
| GBP   | £0.30  | ✅ £24.99 |
| BRL   | R$2.00 | ✅ R$149.90 |
| CVE   | ❌ Não suportado | - |

### Zero-Decimal Currencies

Algumas moedas não usam decimais (ex: JPY, KRW):
- JPY: ¥50 (não ¥0.50)
- KRW: ₩500 (não ₩0.50)

---

## 🌍 Alternativas para CVE

### Opção A: Usar EUR
Cabo Verde está próximo da Europa, EUR é amplamente aceito.

**Vantagens:**
- ✅ Suportado pelo Stripe
- ✅ Estável
- ✅ Aceito internacionalmente

### Opção B: Usar USD
Dólar americano é a moeda de reserva global.

**Vantagens:**
- ✅ Suportado pelo Stripe
- ✅ Amplamente aceito
- ✅ Familiar para desenvolvedores

### Opção C: Gateway Local
Se precisar aceitar CVE:
- Usar gateway de pagamento local de Cabo Verde
- Implementar integração customizada
- Converter CVE → EUR/USD no backend

---

## 🔄 Conversão CVE → EUR

Taxa aproximada (2026):
- 1 EUR ≈ 110 CVE
- 49 CVE ≈ 0,44 EUR

**Tabela de conversão:**

| CVE | EUR |
|-----|-----|
| 49  | 0,44 ❌ (muito baixo) |
| 100 | 0,91 ✅ |
| 500 | 4,55 ✅ |
| 3.300 | 30,00 ✅ |

---

## 📝 Checklist

- [ ] Executar `fix_plan_currency.sql`
- [ ] Verificar planos no banco de dados
- [ ] Testar criação de Payment Intent
- [ ] Verificar valor mínimo (≥ 0,50 EUR)
- [ ] Completar pagamento teste
- [ ] Verificar webhook processa corretamente
- [ ] Atualizar frontend se necessário

---

## 🐛 Troubleshooting

### Erro persiste após atualizar moeda

**Causa:** Cache ou sessão antiga

**Solução:**
1. Limpar checkout sessions antigas:
```sql
DELETE FROM checkout_sessions WHERE status = 'PENDING';
```

2. Reiniciar aplicação

### Valor ainda em CVE

**Causa:** Plano não foi atualizado

**Solução:**
```sql
-- Verificar plano específico
SELECT * FROM platform_plans WHERE name = 'GROWTH';

-- Forçar atualização
UPDATE platform_plans 
SET currency = 'EUR', monthly_price = 29.99 
WHERE name = 'GROWTH';
```

### Frontend mostra valor errado

**Causa:** Frontend pode ter cache

**Solução:**
1. Limpar cache do navegador
2. Recarregar página (Ctrl+F5)
3. Verificar API retorna valor correto:
```bash
GET /api/v1/billing/plans
```

---

## 📚 Referências

- [Stripe Supported Currencies](https://stripe.com/docs/currencies)
- [Stripe Minimum Charge Amounts](https://stripe.com/docs/currencies#minimum-and-maximum-charge-amounts)
- [Stripe Payment Intents](https://stripe.com/docs/payments/payment-intents)
