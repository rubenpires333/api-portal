# Fluxo de Aprovação de APIs

## Visão Geral

Sistema implementado para controlar a publicação de APIs através de aprovação do administrador. Isso permite:
- Controle de qualidade das APIs publicadas
- Preparação para sistema de planos (limitar APIs por plano do provider)
- Auditoria completa do processo de aprovação

## Novos Status

### ApiStatus (Enum)
- `DRAFT` - Rascunho, ainda não solicitada publicação
- `PENDING_APPROVAL` - Aguardando aprovação do administrador ⭐ NOVO
- `PUBLISHED` - Aprovada e publicada
- `REJECTED` - Rejeitada pelo administrador ⭐ NOVO
- `DEPRECATED` - Descontinuada mas ainda funcional
- `ARCHIVED` - Arquivada, não disponível

## Fluxo Completo

### 1. Provider Cria API
```
POST /api/v1/apis
Status inicial: DRAFT
```

### 2. Provider Solicita Publicação
```
PATCH /api/v1/apis/{id}/publish
Status: DRAFT → PENDING_APPROVAL
Campo preenchido: requested_approval_at
```

### 3. Admin Aprova ou Rejeita

#### Aprovar:
```
PATCH /api/v1/admin/apis/{id}/approve
Status: PENDING_APPROVAL → PUBLISHED
Campos preenchidos:
- approved_at
- approved_by (keycloakId do admin)
- published_at
```

#### Rejeitar:
```
PATCH /api/v1/admin/apis/{id}/reject
Body: { "reason": "Motivo da rejeição" }
Status: PENDING_APPROVAL → REJECTED
Campos preenchidos:
- rejected_at
- rejected_by (keycloakId do admin)
- rejection_reason
```

## Novos Campos na Entidade Api

```java
private LocalDateTime requestedApprovalAt;  // Quando foi solicitada
private LocalDateTime approvedAt;            // Quando foi aprovada
private String approvedBy;                   // Quem aprovou (keycloakId)
private LocalDateTime rejectedAt;            // Quando foi rejeitada
private String rejectedBy;                   // Quem rejeitou (keycloakId)
private String rejectionReason;              // Motivo da rejeição
```

## Endpoints Admin

### Listar APIs Pendentes
```
GET /api/v1/admin/apis/pending
Permissão: admin.apis.manage
Retorna: Page<ApiResponse>
```

### Contar APIs Pendentes
```
GET /api/v1/admin/apis/pending/count
Permissão: admin.apis.manage
Retorna: Long
```

### Aprovar API
```
PATCH /api/v1/admin/apis/{id}/approve
Permissão: admin.apis.manage
Retorna: ApiResponse
```

### Rejeitar API
```
PATCH /api/v1/admin/apis/{id}/reject
Body: { "reason": "string" }
Permissão: admin.apis.manage
Retorna: ApiResponse
```

## Permissões

Nova permissão criada:
- `admin.apis.manage` - Gerenciar aprovações de APIs
- Atribuída automaticamente ao role ADMIN

## Migration SQL

Execute o script:
```bash
psql -U postgres -d db_portal_api -f scripts/ADD_API_APPROVAL_FLOW.sql
```

O script:
- Adiciona novos campos na tabela `apis`
- Cria índices para otimizar consultas
- Cria permissão `admin.apis.manage`
- Atribui permissão ao role ADMIN

## Próximos Passos

### Frontend - Provider
- [ ] Atualizar componente de listagem de APIs para mostrar status
- [ ] Adicionar badges visuais (Pendente, Aprovada, Rejeitada)
- [ ] Mostrar motivo de rejeição quando aplicável
- [ ] Desabilitar botão "Publicar" quando status = PENDING_APPROVAL

### Frontend - Admin
- [ ] Criar página "APIs Pendentes de Aprovação"
- [ ] Adicionar badge no menu com contador de pendentes
- [ ] Modal para aprovar/rejeitar com campo de motivo
- [ ] Notificações quando nova API é solicitada

### Sistema de Planos (Futuro)
- [ ] Criar entidade `ProviderPlan` (FREE, BASIC, PREMIUM)
- [ ] Adicionar campo `max_apis` por plano
- [ ] Validar limite antes de permitir criar nova API
- [ ] Dashboard para provider ver uso do plano

## Benefícios

✅ Controle de qualidade das APIs publicadas
✅ Auditoria completa (quem aprovou/rejeitou e quando)
✅ Base para sistema de monetização
✅ Proteção contra spam de APIs
✅ Histórico de rejeições para feedback ao provider
