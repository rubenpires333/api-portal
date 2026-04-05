# Mapeamento de Status de Levantamento

## 📊 Status do Sistema vs Status para Provider

### Status Internos (Backend)
```java
public enum WithdrawalStatus {
    PENDING_APPROVAL,  // Aguardando aprovação do admin
    APPROVED,          // Aprovado, aguardando processamento (invisível para provider)
    PROCESSING,        // Em processamento (REMOVIDO - não usado mais)
    COMPLETED,         // Concluído com sucesso
    REJECTED,          // Rejeitado pelo admin
    CANCELLED          // Cancelado pelo provider
}
```

### Status Visíveis para Provider (Frontend)

| Status Backend | Status Frontend | Emoji | Descrição | Ações Disponíveis |
|----------------|-----------------|-------|-----------|-------------------|
| `PENDING_APPROVAL` | **Pendente** | ⏸️ | Aguardando aprovação do administrador | Cancelar |
| `APPROVED` | **Aprovado** | ✅ | Aprovado e será processado em breve | - |
| `COMPLETED` | **Concluído** | ✅ | Pagamento enviado com sucesso | Ver detalhes |
| `REJECTED` | **Rejeitado** | ❌ | Rejeitado pelo administrador | Ver motivo |
| `CANCELLED` | **Cancelado** | 🚫 | Cancelado por você | - |

### Status Visíveis para Admin (Frontend)

| Status Backend | Status Frontend | Emoji | Descrição | Ações Disponíveis |
|----------------|-----------------|-------|-----------|-------------------|
| `PENDING_APPROVAL` | **Pendente de Aprovação** | ⏸️ | Aguardando sua aprovação | Aprovar / Rejeitar |
| `APPROVED` | **Aprovado** | ✓ | Aprovado, será processado automaticamente | - |
| `COMPLETED` | **Concluído** | ✅ | Processado com sucesso | Ver detalhes |
| `REJECTED` | **Rejeitado** | ❌ | Rejeitado | Ver motivo |
| `CANCELLED` | **Cancelado** | 🚫 | Cancelado pelo provider | - |

---

## 🔄 Fluxo de Status

### Fluxo Normal (Sucesso)
```
Provider solicita
    ↓
PENDING_APPROVAL (Pendente)
    ↓
Admin aprova
    ↓
APPROVED (Aprovado) ← Provider vê "Aprovado"
    ↓
Job processa (1 minuto)
    ↓
COMPLETED (Concluído) ← Provider vê "Concluído"
    ↓
Provider recebe notificação
```

### Fluxo com Rejeição
```
Provider solicita
    ↓
PENDING_APPROVAL (Pendente)
    ↓
Admin rejeita
    ↓
REJECTED (Rejeitado)
    ↓
Saldo devolvido
    ↓
Provider recebe notificação com motivo
```

### Fluxo com Cancelamento
```
Provider solicita
    ↓
PENDING_APPROVAL (Pendente)
    ↓
Provider cancela
    ↓
CANCELLED (Cancelado)
    ↓
Saldo devolvido
```

### Fluxo com Falha no Processamento
```
Provider solicita
    ↓
PENDING_APPROVAL (Pendente)
    ↓
Admin aprova
    ↓
APPROVED (Aprovado)
    ↓
Job tenta processar → FALHA
    ↓
APPROVED (mantém) ← Tenta novamente no próximo ciclo
    ↓
Job tenta novamente (1 minuto depois)
    ↓
COMPLETED (se sucesso) ou APPROVED (se falha)
```

---

## 💬 Mensagens Amigáveis

### Para Provider

#### Status: Pendente
```
Título: Levantamento Pendente
Mensagem: Seu levantamento de €200.00 está aguardando aprovação do administrador.
Tempo estimado: 1-24 horas
Ação: Cancelar levantamento
```

#### Status: Aprovado
```
Título: Levantamento Aprovado
Mensagem: Seu levantamento de €200.00 foi aprovado! O pagamento será processado em breve.
Tempo estimado: 1-5 minutos
Ação: -
```

#### Status: Concluído
```
Título: Levantamento Concluído
Mensagem: Seu levantamento de €200.00 foi processado com sucesso! 
Valor líquido: €193.00
Taxa: €7.00
Método: Transferência Bancária
Tempo de chegada: 2-3 dias úteis
Ação: Ver comprovante
```

#### Status: Rejeitado
```
Título: Levantamento Rejeitado
Mensagem: Seu levantamento de €200.00 foi rejeitado.
Motivo: Documentação incompleta
Saldo devolvido: €200.00
Ação: Solicitar novo levantamento
```

#### Status: Cancelado
```
Título: Levantamento Cancelado
Mensagem: Você cancelou o levantamento de €200.00.
Saldo devolvido: €200.00
Ação: Solicitar novo levantamento
```

### Para Admin

#### Status: Pendente de Aprovação
```
Título: Novo Levantamento
Provider: João Silva (joao@email.com)
Valor: €200.00
Valor líquido: €193.00
Taxa: €7.00
Método: Transferência Bancária
Saldo disponível: €500.00
Ações: [Aprovar] [Rejeitar]
```

#### Status: Aprovado
```
Título: Levantamento Aprovado
Provider: João Silva
Valor: €200.00
Aprovado por: Admin (você)
Status: Será processado automaticamente
Próxima tentativa: Em 1 minuto
```

#### Status: Concluído
```
Título: Levantamento Concluído
Provider: João Silva
Valor: €200.00
Processado em: 05/04/2026 14:00
Método: Stripe Payout
Payout ID: po_1ABC123...
```

---

## 🎨 Cores e Ícones (Sugestão para Frontend)

### Cores
```css
.status-pending { color: #FFA500; } /* Laranja */
.status-approved { color: #4CAF50; } /* Verde claro */
.status-completed { color: #2E7D32; } /* Verde escuro */
.status-rejected { color: #F44336; } /* Vermelho */
.status-cancelled { color: #9E9E9E; } /* Cinza */
```

### Ícones (Material Icons)
```
PENDING_APPROVAL: hourglass_empty
APPROVED: check_circle_outline
COMPLETED: check_circle
REJECTED: cancel
CANCELLED: block
```

---

## 📱 Exemplo de Implementação (Frontend)

### TypeScript/Angular
```typescript
export enum WithdrawalStatus {
  PENDING_APPROVAL = 'PENDING_APPROVAL',
  APPROVED = 'APPROVED',
  COMPLETED = 'COMPLETED',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED'
}

export const WITHDRAWAL_STATUS_LABELS = {
  [WithdrawalStatus.PENDING_APPROVAL]: 'Pendente',
  [WithdrawalStatus.APPROVED]: 'Aprovado',
  [WithdrawalStatus.COMPLETED]: 'Concluído',
  [WithdrawalStatus.REJECTED]: 'Rejeitado',
  [WithdrawalStatus.CANCELLED]: 'Cancelado'
};

export const WITHDRAWAL_STATUS_COLORS = {
  [WithdrawalStatus.PENDING_APPROVAL]: 'warning',
  [WithdrawalStatus.APPROVED]: 'success',
  [WithdrawalStatus.COMPLETED]: 'success',
  [WithdrawalStatus.REJECTED]: 'danger',
  [WithdrawalStatus.CANCELLED]: 'secondary'
};

export const WITHDRAWAL_STATUS_ICONS = {
  [WithdrawalStatus.PENDING_APPROVAL]: 'hourglass_empty',
  [WithdrawalStatus.APPROVED]: 'check_circle_outline',
  [WithdrawalStatus.COMPLETED]: 'check_circle',
  [WithdrawalStatus.REJECTED]: 'cancel',
  [WithdrawalStatus.CANCELLED]: 'block'
};
```

### HTML Template
```html
<div class="withdrawal-status" 
     [ngClass]="'status-' + withdrawal.status.toLowerCase()">
  <mat-icon>{{ WITHDRAWAL_STATUS_ICONS[withdrawal.status] }}</mat-icon>
  <span>{{ WITHDRAWAL_STATUS_LABELS[withdrawal.status] }}</span>
</div>
```

---

## ✅ Resumo

### O que mudou:
1. ❌ **PROCESSING removido** - Provider não vê mais esse status
2. ✅ **APPROVED é temporário** - Provider vê "Aprovado" por ~1 minuto
3. ✅ **COMPLETED é final** - Provider vê "Concluído" quando payout é feito
4. ✅ **Mensagens amigáveis** - Textos claros e informativos
5. ✅ **Cores e ícones** - Visual intuitivo

### Fluxo simplificado para Provider:
```
Pendente → Aprovado → Concluído
   ⏸️        ✅         ✅
```

**Sistema mais simples e intuitivo!** 🎉
