# Subscription Module

Módulo de gestão de subscrições de APIs. Permite que consumers subscrevam APIs e recebam API Keys para acesso via Gateway.

## Arquitetura

```
subscription/
├── controller/
│   └── SubscriptionController.java    ← Endpoints REST
├── domain/
│   ├── entity/
│   │   └── Subscription.java          ← Entidade JPA
│   ├── enums/
│   │   └── SubscriptionStatus.java    ← Estados da subscrição
│   └── repository/
│       └── SubscriptionRepository.java ← Queries JPA
├── dto/
│   ├── SubscriptionRequest.java       ← Request para subscrever
│   ├── SubscriptionResponse.java      ← Response com dados
│   └── RevokeRequest.java             ← Request para revogar
└── service/
    └── SubscriptionService.java       ← Lógica de negócio
```

## Fluxo de Subscrição

### 1. Consumer Subscreve API
```http
POST /api/v1/subscriptions
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "apiId": "uuid-da-api",
  "notes": "Vou usar para integração com sistema X"
}
```

**Resposta:**
```json
{
  "id": "uuid",
  "apiId": "uuid-da-api",
  "apiName": "Nome da API",
  "apiSlug": "nome-api",
  "status": "ACTIVE",
  "apiKey": "apk_abc123...",
  "createdAt": "2026-03-29T10:00:00"
}
```

### 2. Consumer Usa API Key
```http
GET https://gateway.apiportal.cv/api/v2/nome-api/endpoint
X-API-Key: apk_abc123...
```

### 3. Gateway Valida API Key
- Verifica se API Key existe e está ativa
- Verifica se subscrição é para a API solicitada
- Registra chamada no audit log
- Faz proxy para API do provider

## Estados da Subscrição

| Status | Descrição | Pode usar API? |
|--------|-----------|----------------|
| PENDING | Aguardando aprovação do provider | ❌ Não |
| ACTIVE | Ativa e funcional | ✅ Sim |
| REVOKED | Revogada pelo provider | ❌ Não |
| EXPIRED | Expirada por tempo | ❌ Não |
| CANCELLED | Cancelada pelo consumer | ❌ Não |

## Endpoints

### Consumer

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/subscriptions` | Subscrever uma API |
| GET | `/api/v1/subscriptions` | Listar minhas subscrições |
| GET | `/api/v1/subscriptions/{id}` | Detalhes de uma subscrição |
| DELETE | `/api/v1/subscriptions/{id}` | Cancelar subscrição |

### Provider

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/v1/subscriptions/provider` | Listar subscrições das minhas APIs |
| GET | `/api/v1/subscriptions/provider?status=PENDING` | Filtrar por status |
| PUT | `/api/v1/subscriptions/provider/{id}/approve` | Aprovar subscrição |
| PUT | `/api/v1/subscriptions/provider/{id}/revoke` | Revogar subscrição |

## Geração de API Keys

API Keys são geradas automaticamente no formato:
```
apk_{uuid_sem_hifens}
```

Exemplo: `apk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6`

## Validação no Gateway

O `GatewayService` valida a API Key em cada requisição:

1. Extrai header `X-API-Key`
2. Chama `subscriptionService.validateApiKey(apiKey)`
3. Verifica se subscrição está ACTIVE
4. Verifica se API Key é para a API solicitada
5. Registra chamada no audit log com dados do consumer

## Cache Redis

API Keys são cacheadas para performance:
- Cache key: `apiKeys::{apiKey}`
- TTL: 10 minutos
- Invalidado ao revogar ou cancelar

## Segurança

- API Keys são únicas (constraint no banco)
- Apenas consumers autenticados podem subscrever
- Providers só veem subscrições das suas APIs
- API Keys não expiram por padrão (campo `expiresAt` é opcional)

## Próximas Melhorias

- [ ] Aprovação manual (atualmente é automática)
- [ ] Rate limiting por subscrição
- [ ] Rotação de API Keys
- [ ] Expiração automática
- [ ] Notificações de novas subscrições
- [ ] Webhooks para eventos de subscrição

## Testes

```bash
# Compilar
mvn clean compile

# Executar testes
mvn test -Dtest=SubscriptionServiceTest

# Testar endpoint
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer {jwt}" \
  -H "Content-Type: application/json" \
  -d '{"apiId":"uuid-da-api"}'
```

## Troubleshooting

**Erro: "API Key inválida ou inativa"**
- Verificar se subscrição está ACTIVE
- Verificar se API Key está correta (copiar do portal)
- Verificar se API Key é para a API solicitada

**Erro: "Já existe uma subscrição ativa"**
- Consumer já tem subscrição ativa para esta API
- Cancelar subscrição antiga antes de criar nova

**Erro: "Apenas APIs publicadas podem ser subscritas"**
- API deve estar com status PUBLISHED
- Provider deve publicar a API primeiro
