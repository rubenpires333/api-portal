# Funcionalidades Pendentes - API Portal

## 🔴 CRÍTICO (Menu já existe mas sem implementação)

### 1. Estatísticas (`/provider/statistics`)
**Status:** Não implementado  
**Prioridade:** Alta

**Frontend:**
- Componente não existe
- Dashboard com métricas principais
- Gráficos de uso ao longo do tempo (Chart.js ou ApexCharts)
- Filtros por período (hoje, semana, mês, ano)

**Métricas necessárias:**
- Total de chamadas por API
- Latência média por endpoint
- Taxa de erro (4xx, 5xx)
- Top consumers (quem mais usa)
- Endpoints mais utilizados
- Distribuição de métodos HTTP

**Backend:**
- Agregação de dados do `audit_logs`
- Endpoints: `/api/v1/analytics/provider/summary`, `/api/v1/analytics/provider/apis/{apiId}`

---

### 2. Subscrições (`/provider/subscriptions`)
**Status:** ✅ IMPLEMENTADO  
**Prioridade:** Crítica

**Frontend:**
- ✅ Componente criado (`ProviderSubscriptionsComponent`)
- ✅ Listar consumers que subscreveram suas APIs
- ✅ Ver detalhes de cada subscrição (API Key, data, status)
- ✅ Aprovar pedidos pendentes
- ✅ Revogar acessos com motivo
- ✅ Filtros por status (Todas, Pendentes, Ativas, Revogadas)
- ✅ Busca por API, email ou nome do consumer
- ✅ Copiar API Key
- ✅ Paginação

**Backend:**
- ✅ Módulo completo de subscrições
- ✅ Entidade `Subscription` com relacionamento com `Api`
- ✅ Enum `SubscriptionStatus` (PENDING, ACTIVE, REVOKED, EXPIRED, CANCELLED)
- ✅ Geração automática de API Keys (`apk_` + UUID)
- ✅ Endpoints REST completos:
  - `POST /api/v1/subscriptions` - Consumer subscreve API
  - `GET /api/v1/subscriptions` - Listar minhas subscrições (consumer)
  - `DELETE /api/v1/subscriptions/{id}` - Cancelar subscrição (consumer)
  - `GET /api/v1/subscriptions/provider` - Listar subscrições do provider
  - `PUT /api/v1/subscriptions/provider/{id}/approve` - Aprovar subscrição
  - `PUT /api/v1/subscriptions/provider/{id}/revoke` - Revogar subscrição
- ✅ Validação de API Key (método `validateApiKey`)
- ✅ Cache Redis preparado
- ✅ Audit log automático via JPA Auditing

**Consumer:**
- ✅ Componente criado (`ConsumerSubscriptionsComponent`)
- ✅ Listar minhas subscrições
- ✅ Ver detalhes e API Key
- ✅ Cancelar subscrição
- ✅ Rotas: `/consumer/subscriptions`, `/consumer/subscriptions/active`, etc.

**Próximos passos:**
- Integrar validação de API Key no Gateway
- Adicionar rate limiting por subscrição
- Notificações de novas subscrições

---

### 3. Perfil (`/provider/profile`)
**Status:** Não implementado  
**Prioridade:** Média

**Frontend:**
- Formulário de perfil do provider
- Upload de logo/avatar
- Informações da empresa (nome, website, descrição)
- Dados de contacto (email, telefone)
- Configurações de notificações

**Backend:**
- Entidade `Provider` com perfil detalhado
- Endpoints: `GET/PUT /api/v1/providers/me`
- Upload de imagens (logo, avatar)
- Validação de dados

---

## 🟡 BACKEND FALTANTE

### 4. Subscription Module (Backend)
**Status:** ✅ EM IMPLEMENTAÇÃO  
**Prioridade:** Crítica

**Entidades:**
- `Subscription` - relação entre Consumer e API
- `ApiKey` - chaves de acesso geradas
- `SubscriptionStatus` - enum (PENDING, ACTIVE, REVOKED, EXPIRED)

**Endpoints:**
```
POST   /api/v1/subscriptions              → Consumer subscreve API
GET    /api/v1/subscriptions              → Listar minhas subscrições
GET    /api/v1/subscriptions/{id}         → Detalhes da subscrição
DELETE /api/v1/subscriptions/{id}         → Cancelar subscrição

GET    /api/v1/providers/subscriptions    → Provider vê suas subscrições
PUT    /api/v1/providers/subscriptions/{id}/approve  → Aprovar pedido
PUT    /api/v1/providers/subscriptions/{id}/revoke   → Revogar acesso
```

**Funcionalidades:**
- Geração automática de API Key (UUID ou token seguro)
- Validação de API Key no gateway
- Cache Redis para performance
- Audit log de todas as operações

---

### 5. Analytics Module (Backend)
**Status:** Não implementado  
**Prioridade:** Alta

**Entidades:**
- `ApiMetrics` - métricas agregadas por API
- `EndpointMetrics` - métricas por endpoint
- Agregação diária/horária

**Endpoints:**
```
GET /api/v1/analytics/provider/summary           → Resumo geral
GET /api/v1/analytics/provider/apis/{apiId}      → Métricas de uma API
GET /api/v1/analytics/provider/endpoints/{id}    → Métricas de endpoint
GET /api/v1/analytics/consumer/usage             → Consumer vê seu uso
```

**Funcionalidades:**
- Job agendado para agregar dados do `audit_logs`
- Cálculo de latência média, taxa de erro
- Ranking de endpoints mais usados
- Exportação de relatórios (CSV, PDF)

---

### 6. User/Provider Profile Module (Backend)
**Status:** Parcialmente implementado  
**Prioridade:** Média

**Entidades:**
- `Provider` - perfil detalhado do fornecedor
- `ProviderSettings` - configurações

**Endpoints:**
```
GET    /api/v1/providers/me        → Meu perfil
PUT    /api/v1/providers/me        → Atualizar perfil
POST   /api/v1/providers/logo      → Upload de logo
DELETE /api/v1/providers/logo      → Remover logo
```

**Campos do Provider:**
- Nome da empresa
- Website
- Descrição
- Logo URL
- Email de contacto
- Telefone
- Endereço

---

## 🟢 MELHORIAS OPCIONAIS

### 7. Documentação Interativa
**Status:** Não implementado  
**Prioridade:** Baixa

- Swagger UI embutido na página da API
- Testar endpoints diretamente no portal
- Geração automática de SDKs (Python, JavaScript, Java)
- Exemplos de código em múltiplas linguagens

---

### 8. Versionamento Avançado
**Status:** Básico implementado  
**Prioridade:** Baixa

- Comparação visual entre versões (diff)
- Changelog automático baseado em mudanças
- Notificar consumers sobre breaking changes
- Migração assistida entre versões

---

### 9. Notificações
**Status:** Não implementado  
**Prioridade:** Média

**Tipos de notificação:**
- Email quando alguém subscreve
- Alertas de uso excessivo (rate limit)
- Notificações de erros críticos (taxa de erro > 10%)
- Lembretes de APIs depreciadas

**Backend:**
- Integração com serviço de email (SendGrid, AWS SES)
- Templates de email
- Fila de notificações (RabbitMQ ou Redis)

---

### 10. Rate Limiting
**Status:** Não implementado  
**Prioridade:** Média

**Funcionalidades:**
- Configurar limites por API/endpoint
- Diferentes tiers (free: 100/dia, pro: 10k/dia, enterprise: ilimitado)
- Rate limit por API Key
- Headers de resposta: `X-RateLimit-Limit`, `X-RateLimit-Remaining`

**Implementação:**
- Redis para contadores
- Bucket4j ou Spring Cloud Gateway Rate Limiter
- Configuração por subscrição

---

### 11. Webhooks
**Status:** Não implementado  
**Prioridade:** Baixa

- Consumers configuram webhooks para eventos
- Eventos: nova versão, API depreciada, limite atingido
- Retry automático em caso de falha
- Logs de entregas

---

### 12. Sandbox/Mock
**Status:** Não implementado  
**Prioridade:** Baixa

- Ambiente de testes sem afetar produção
- Mock responses configuráveis
- Simulação de erros e latência

---

## 📊 RESUMO

| Módulo | Status | Prioridade | Estimativa | Progresso |
|--------|--------|------------|------------|-----------|
| Subscription Module | ✅ Implementado | Crítica | 2-3 dias | 100% |
| Analytics Module | 🔴 Não iniciado | Alta | 3-4 dias | 0% |
| Provider Profile | 🔴 Não iniciado | Média | 1-2 dias | 0% |
| Statistics (Frontend) | 🔴 Não iniciado | Alta | 2 dias | 0% |
| Subscriptions (Frontend) | ✅ Implementado | Crítica | 1 dia | 100% |
| Profile (Frontend) | 🔴 Não iniciado | Média | 1 dia | 0% |
| Rate Limiting | 🔴 Não iniciado | Média | 2-3 dias | 0% |
| Notificações | 🔴 Não iniciado | Média | 2 dias | 0% |
| Documentação Interativa | 🔴 Não iniciado | Baixa | 3-4 dias | 0% |
| Webhooks | 🔴 Não iniciado | Baixa | 2-3 dias | 0% |

**Total estimado:** 17-24 dias de desenvolvimento restantes

---

## 🎯 ROADMAP SUGERIDO

### Sprint 1 (Semana 1-2)
1. ✅ Subscription Module (backend + frontend)
2. Provider Profile (backend + frontend)

### Sprint 2 (Semana 3-4)
3. Analytics Module (backend)
4. Statistics (frontend)

### Sprint 3 (Semana 5-6)
5. Rate Limiting
6. Notificações básicas

### Sprint 4 (Semana 7+)
7. Melhorias opcionais conforme necessidade

---

**Última atualização:** 29/03/2026
