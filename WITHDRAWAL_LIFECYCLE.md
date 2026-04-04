# Ciclo de Vida do Levantamento

## 📊 Fluxo Completo

### 1. Solicitação de Levantamento (Provider)
```
Status: PENDING_APPROVAL
Saldo: Disponível → Reservado
Transação: RESERVED
```

**O que acontece:**
- Provider solicita levantamento de €X
- Sistema valida saldo disponível
- Calcula taxas (percentual + fixa)
- Move saldo de `availableBalance` para `reservedBalance`
- Cria `WithdrawalRequest` com status `PENDING_APPROVAL`
- Cria `WalletTransaction` com status `RESERVED` e valor negativo
- Notifica todos os administradores

**Exemplo:**
```
Saldo Disponível: €250.00 → €150.00
Saldo Reservado:  €0.00   → €100.00
```

---

### 2. Aprovação pelo Administrador
```
Status: PENDING_APPROVAL → APPROVED
Saldo: Reservado → Removido (sai da carteira)
Transação: RESERVED → COMPLETED
```

**O que acontece:**
- Admin aprova o levantamento
- Sistema remove o saldo reservado (dinheiro sai da carteira)
- Atualiza `WithdrawalRequest` para status `APPROVED`
- Atualiza `WalletTransaction` para status `COMPLETED`
- Define `processedAt` com timestamp atual
- Registra `approvedBy` (ID do admin)

**Exemplo:**
```
Saldo Reservado: €100.00 → €0.00
(O dinheiro foi transferido para o provider)
```

---

### 3. Rejeição pelo Administrador
```
Status: PENDING_APPROVAL → REJECTED
Saldo: Reservado → Disponível (devolvido)
Transação: RESERVED → CANCELLED
```

**O que acontece:**
- Admin rejeita o levantamento com motivo
- Sistema devolve saldo de `reservedBalance` para `availableBalance`
- Atualiza `WithdrawalRequest` para status `REJECTED`
- Atualiza `WalletTransaction` para status `CANCELLED`
- Registra `rejectionReason` e `approvedBy` (ID do admin)

**Exemplo:**
```
Saldo Disponível: €150.00 → €250.00
Saldo Reservado:  €100.00 → €0.00
(O dinheiro voltou para disponível)
```

---

### 4. Cancelamento pelo Provider
```
Status: PENDING_APPROVAL → CANCELLED
Saldo: Reservado → Disponível (devolvido)
Transação: RESERVED → CANCELLED
```

**O que acontece:**
- Provider cancela sua própria solicitação (apenas se PENDING)
- Sistema devolve saldo de `reservedBalance` para `availableBalance`
- Atualiza `WithdrawalRequest` para status `CANCELLED`
- Atualiza `WalletTransaction` para status `CANCELLED`

---

## 🔄 Estados do Saldo

### Saldo Disponível (availableBalance)
- Pode ser usado para novos levantamentos
- Aumenta quando: recebe pagamentos, rejeição/cancelamento de levantamento
- Diminui quando: solicita levantamento

### Saldo Reservado (reservedBalance)
- Não pode ser usado para novos levantamentos
- Aumenta quando: solicita levantamento
- Diminui quando: aprovação (sai da carteira), rejeição/cancelamento (volta para disponível)

### Saldo Pendente (pendingBalance)
- Em holdback (14 dias após pagamento)
- Não pode ser levantado ainda
- Após 14 dias, move para `availableBalance`

---

## 📈 Estados da Transação

| Status | Descrição | Quando |
|--------|-----------|--------|
| `PENDING` | Em holdback | Após receber pagamento |
| `AVAILABLE` | Disponível | Após 14 dias do pagamento |
| `RESERVED` | Reservado | Durante solicitação de levantamento |
| `COMPLETED` | Completado | Após aprovação do levantamento |
| `CANCELLED` | Cancelado | Após rejeição ou cancelamento |

---

## 🎯 Estados do WithdrawalRequest

| Status | Descrição | Ações Disponíveis |
|--------|-----------|-------------------|
| `PENDING_APPROVAL` | Aguardando aprovação | Aprovar, Rejeitar, Cancelar (provider) |
| `APPROVED` | Aprovado e processado | Nenhuma |
| `REJECTED` | Rejeitado pelo admin | Nenhuma |
| `CANCELLED` | Cancelado pelo provider | Nenhuma |

---

## 💰 Exemplo Completo

### Situação Inicial
```
Saldo Disponível: €500.00
Saldo Reservado:  €0.00
Saldo Pendente:   €100.00
```

### Provider Solicita €200
```
Valor Solicitado: €200.00
Taxa (2% + €1):   €5.00
Valor Líquido:    €195.00

Saldo Disponível: €500.00 → €300.00
Saldo Reservado:  €0.00   → €200.00
```

### Admin Aprova
```
Saldo Disponível: €300.00 (sem alteração)
Saldo Reservado:  €200.00 → €0.00

Provider recebe: €195.00 (via método escolhido)
```

### Situação Final
```
Saldo Disponível: €300.00
Saldo Reservado:  €0.00
Saldo Pendente:   €100.00
```

---

## ⚠️ Regras Importantes

1. **Não pode solicitar levantamento maior que saldo disponível**
2. **Saldo reservado não pode ser usado para novos levantamentos**
3. **Apenas levantamentos PENDING podem ser cancelados pelo provider**
4. **Apenas levantamentos PENDING podem ser aprovados/rejeitados pelo admin**
5. **Após aprovação, o dinheiro SAI da carteira (não fica reservado)**
6. **Após rejeição/cancelamento, o dinheiro VOLTA para disponível**

---

## 🔍 Verificação de Integridade

Para verificar se o sistema está correto:

```sql
-- O total deve sempre bater
SELECT 
    available_balance + reserved_balance + pending_balance as total_in_wallet,
    lifetime_earned
FROM provider_wallets
WHERE provider_id = 'xxx';

-- Transações reservadas devem ter withdrawal pendente
SELECT 
    wt.id as transaction_id,
    wt.status as transaction_status,
    wr.id as withdrawal_id,
    wr.status as withdrawal_status
FROM wallet_transactions wt
LEFT JOIN withdrawal_requests wr ON wr.id = wt.reference_id
WHERE wt.status = 'RESERVED';
```
