# Saldo Reservado - Como Funciona

## 💰 Tipos de Saldo na Carteira

```
┌─────────────────────────────────────┐
│     CARTEIRA DO PROVIDER            │
├─────────────────────────────────────┤
│ Disponível:  500,00 €  ✅          │ ← Pode usar/levantar
│ Reservado:   475,00 €  🔒          │ ← Levantamentos em processo
│ Pendente:    100,00 €  ⏳          │ ← Em holdback (14 dias)
├─────────────────────────────────────┤
│ TOTAL:     1.075,00 €               │
└─────────────────────────────────────┘
```

---

## 🔒 O que é Saldo Reservado?

**Saldo Reservado** = Dinheiro que está "bloqueado" para levantamentos que foram:
- ✅ Solicitados pelo provider
- ✅ Aprovados pelo admin (ou auto-aprovados)
- ⏳ Aguardando processamento pelo job

### Por que Reservar?

1. **Evitar duplo gasto** - Provider não pode usar o mesmo dinheiro 2x
2. **Garantir disponibilidade** - Quando job processar, dinheiro está lá
3. **Transparência** - Provider sabe exatamente quanto está "em trânsito"

---

## 🔄 Ciclo de Vida do Saldo

### 1. Saldo Disponível → Reservado (Solicitação)

```
Provider solicita levantamento de 200 EUR
    ↓
Sistema valida saldo disponível (>= 200 EUR)
    ↓
Cria registro de levantamento (PENDING_APPROVAL)
    ↓
MOVE SALDO:
  available_balance: 500 → 300 EUR
  reserved_balance:    0 → 200 EUR
```

### 2. Saldo Reservado → Debitado (Conclusão)

```
Admin aprova levantamento
    ↓
Status: PENDING_APPROVAL → APPROVED
    ↓
Job processa (a cada 1 minuto)
    ↓
Stripe Payout criado com sucesso
    ↓
Status: APPROVED → COMPLETED
    ↓
REMOVE SALDO:
  reserved_balance: 200 → 0 EUR
  (dinheiro saiu da carteira)
```

### 3. Saldo Reservado → Disponível (Cancelamento/Rejeição)

```
Provider cancela OU Admin rejeita
    ↓
Status: PENDING_APPROVAL → CANCELLED/REJECTED
    ↓
DEVOLVE SALDO:
  reserved_balance: 200 → 0 EUR
  available_balance: 300 → 500 EUR
```

---

## 📊 Exemplo Prático

### Situação Atual: 475 EUR Reservado

Execute o script para ver detalhes:
```bash
psql -U postgres -d api_portal -f scripts/check_reserved_balance.sql
```

**Possíveis Cenários:**

#### Cenário A: Múltiplos Levantamentos Pendentes
```
Levantamento 1: 200 EUR - PENDING_APPROVAL
Levantamento 2: 220 EUR - APPROVED
Levantamento 3:  55 EUR - APPROVED
────────────────────────────────────
TOTAL RESERVADO: 475 EUR ✅
```

#### Cenário B: Levantamentos Falhando
```
Levantamento 1: 220 EUR - APPROVED (tentando processar)
  → Falha: valor abaixo do mínimo Stripe (200 EUR)
  → Fica APPROVED, tenta novamente a cada 1 minuto
  → Saldo continua RESERVADO até sucesso ou cancelamento

Levantamento 2: 255 EUR - APPROVED (tentando processar)
  → Falha: saldo insuficiente no Stripe
  → Fica APPROVED, tenta novamente
  → Saldo continua RESERVADO
────────────────────────────────────
TOTAL RESERVADO: 475 EUR ✅
```

---

## ⚠️ Problemas Comuns

### Problema 1: Saldo Reservado Alto

**Sintoma:** `reserved_balance` alto, provider reclama que não pode levantar

**Causas:**
1. Levantamentos aprovados mas não processados (aguardando job)
2. Levantamentos falhando repetidamente (ex: abaixo do mínimo Stripe)
3. Job parado ou com erro

**Solução:**
```sql
-- Ver levantamentos que estão reservando saldo
SELECT id, requested_amount, status, requested_at
FROM withdrawal_requests
WHERE status IN ('PENDING_APPROVAL', 'APPROVED', 'PROCESSING')
ORDER BY requested_at DESC;

-- Cancelar levantamentos problemáticos
UPDATE withdrawal_requests
SET status = 'CANCELLED'
WHERE id = 'uuid-do-levantamento';

-- Devolver saldo
UPDATE provider_wallets
SET 
    reserved_balance = reserved_balance - 220.00,
    available_balance = available_balance + 220.00
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
```

### Problema 2: Saldo Reservado Não Bate

**Sintoma:** `reserved_balance` diferente da soma dos levantamentos pendentes

**Causa:** Inconsistência no banco (bug, erro de transação, etc.)

**Solução:**
```sql
-- Calcular saldo correto
SELECT 
    COALESCE(SUM(requested_amount), 0) as deveria_ser
FROM withdrawal_requests
WHERE wallet_id = (SELECT id FROM provider_wallets WHERE provider_id = '...')
AND status IN ('PENDING_APPROVAL', 'APPROVED', 'PROCESSING');

-- Corrigir manualmente
UPDATE provider_wallets
SET reserved_balance = 475.00  -- Valor correto
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
```

### Problema 3: Levantamentos Travados em APPROVED

**Sintoma:** Levantamentos ficam em APPROVED por muito tempo

**Causas:**
1. Valor abaixo do mínimo Stripe (200 EUR)
2. Saldo insuficiente no Stripe
3. Erro de configuração (conta bancária, moeda, etc.)

**Solução:**
```bash
# Ver logs do job
grep "Processing withdrawal" logs/application.log | tail -20
grep "Stripe Payout error" logs/application.log | tail -10

# Identificar erro específico
# - amount_too_small → Cancelar e pedir valor maior
# - balance_insufficient → Aguardar saldo no Stripe
# - external accounts → Configurar Stripe Connect
```

---

## ✅ Verificações de Saúde

### 1. Saldo Reservado vs Levantamentos

```sql
-- Deve retornar "✅ Valores batem!"
SELECT 
    reserved_balance as saldo_reservado,
    (SELECT COALESCE(SUM(requested_amount), 0) 
     FROM withdrawal_requests 
     WHERE wallet_id = pw.id 
     AND status IN ('PENDING_APPROVAL', 'APPROVED', 'PROCESSING')) as soma_levantamentos,
    CASE 
        WHEN reserved_balance = (SELECT COALESCE(SUM(requested_amount), 0) 
                                 FROM withdrawal_requests 
                                 WHERE wallet_id = pw.id 
                                 AND status IN ('PENDING_APPROVAL', 'APPROVED', 'PROCESSING'))
        THEN '✅ Valores batem!'
        ELSE '⚠️ Inconsistência detectada'
    END as validacao
FROM provider_wallets pw
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
```

### 2. Levantamentos Antigos Travados

```sql
-- Levantamentos aprovados há mais de 1 hora
SELECT 
    id,
    requested_amount,
    status,
    requested_at,
    NOW() - requested_at as tempo_decorrido
FROM withdrawal_requests
WHERE status = 'APPROVED'
AND requested_at < NOW() - INTERVAL '1 hour'
ORDER BY requested_at ASC;
```

### 3. Job Funcionando

```bash
# Deve mostrar execuções a cada 1 minuto
grep "Starting withdrawal processing job" logs/application.log | tail -5
```

---

## 🎯 Ações Recomendadas

### Para os 475 EUR Reservados:

1. **Verificar detalhes:**
```bash
psql -U postgres -d api_portal -f scripts/check_reserved_balance.sql
```

2. **Identificar levantamentos:**
   - Quantos estão PENDING_APPROVAL? → Aguardar admin aprovar
   - Quantos estão APPROVED? → Aguardar job processar (1 min)
   - Algum falhando? → Ver logs e corrigir

3. **Se levantamentos estão falhando:**
   - Valor < 200 EUR? → Cancelar e solicitar >= 200 EUR
   - Saldo Stripe insuficiente? → Aguardar ou simular (teste)
   - Outro erro? → Ver logs e corrigir configuração

4. **Se tudo OK:**
   - Aguardar job processar (máximo 1 minuto por levantamento)
   - Saldo reservado irá diminuir conforme levantamentos completam
   - Quando COMPLETED, saldo sai do reservado

---

## 📈 Monitoramento

### Dashboard Recomendado

```
┌─────────────────────────────────────┐
│  CARTEIRA - João Silva              │
├─────────────────────────────────────┤
│  Disponível:     500,00 €           │
│  Reservado:      475,00 €  ⚠️       │
│  Pendente:       100,00 €           │
├─────────────────────────────────────┤
│  LEVANTAMENTOS EM PROCESSO:         │
│  • 220 EUR - Aprovado (processando) │
│  • 255 EUR - Aprovado (processando) │
│                                     │
│  [Ver Detalhes] [Cancelar Todos]   │
└─────────────────────────────────────┘
```

### Alertas Automáticos

- ⚠️ Saldo reservado > 50% do total
- ⚠️ Levantamento em APPROVED por > 5 minutos
- ⚠️ Levantamento falhando repetidamente
- ⚠️ Inconsistência entre reserved_balance e soma de levantamentos

---

## ✅ Resumo

**Saldo Reservado = Normal e Esperado**

- ✅ Protege contra duplo gasto
- ✅ Garante disponibilidade para levantamentos
- ✅ Transparente para provider

**475 EUR Reservado significa:**
- Há levantamentos aprovados aguardando processamento
- Job irá processar em até 1 minuto
- Após processamento, saldo sai do reservado

**Para liberar saldo reservado:**
1. Aguardar job processar (automático)
2. Ou cancelar levantamentos manualmente
3. Ou admin rejeitar levantamentos

**Sistema funcionando corretamente!** ✅
