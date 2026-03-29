# Log de Execução do Gateway - Análise de Falha

## Requisição do Frontend
```
Method: GET
URL: http://localhost:8080/gateway/api/api-brasil-feriados-nacionais/feriados/v1/2026
API Key: I9fRaJ6HBcFwiaGRVmIkGFnwaXZtb1x6MCye80IYCEI
```

## Logs do Backend

### 1. Requisição Recebida
```
Gateway request: GET /gateway/api/api-brasil-feriados-nacionais/feriados/v1/2026 for API: api-brasil-feriados-nacionais
Slug: api-brasil-feriados-nacionais
Method: GET
URI: /gateway/api/api-brasil-feriados-nacionais/feriados/v1/2026
Query: null
```

### 2. API Encontrada
```
API encontrada: API Brasil Feriados Nacionais (https://brasilapi.com.br/api)
Target URL: https://brasilapi.com.br/api/feriados/v1/2026
```

### 3. Headers Enviados para API Externa
```
Headers copiados: [user-agent, accept, accept-language, accept-encoding, referer, content-type, origin, connection, sec-fetch-dest, sec-fetch-mode, sec-fetch-site, priority]
```
**NOTA**: Authorization header foi removido pelo interceptor (correto!)

### 4. Resposta da API Externa
```
Gateway response: 200 OK from https://brasilapi.com.br/api/feriados/v1/2026
Response body length: 299
```

### 5. Headers da Resposta Externa
```
Response headers from external API: 
[Date, Content-Type, Transfer-Encoding, Connection, access-control-allow-origin, Age, Cache-Control, Report-To, Server, strict-transport-security, x-matched-path, x-vercel-cache, x-vercel-id, cf-cache-status, Nel, etag, Content-Encoding, CF-RAY, alt-svc]
```

### 6. Headers Após Filtragem
```
Response headers after filtering: [Content-Type]
```
**NOTA**: Removidos todos os headers problemáticos (CORS, Transfer-Encoding, Connection, Content-Encoding)

## Resposta Original da BrasilAPI
```json
[
  {"date":"2026-01-01","name":"Confraternização mundial","type":"national"},
  {"date":"2026-02-17","name":"Carnaval","type":"national"},
  {"date":"2026-04-03","name":"Sexta-feira Santa","type":"national"},
  {"date":"2026-04-05","name":"Páscoa","type":"national"},
  {"date":"2026-04-21","name":"Tiradentes","type":"national"},
  {"date":"2026-05-01","name":"Dia do trabalho","type":"national"},
  {"date":"2026-06-04","name":"Corpus Christi","type":"national"},
  {"date":"2026-09-07","name":"Independência do Brasil","type":"national"},
  {"date":"2026-10-12","name":"Nossa Senhora Aparecida","type":"national"},
  {"date":"2026-11-02","name":"Finados","type":"national"},
  {"date":"2026-11-15","name":"Proclamação da República","type":"national"},
  {"date":"2026-11-20","name":"Dia da consciência negra","type":"national"},
  {"date":"2026-12-25","name":"Natal","type":"national"}
]
```

## Erro no Frontend
```
status: 200
statusText: "Unknown Error"
message: "Http failure during parsing for http://localhost:8080/gateway/api/..."
error: SyntaxError
```

## Análise do Problema

### ✅ O que está funcionando:
1. API Key sendo enviada corretamente
2. Gateway roteando para o controller
3. API externa retornando 200 OK com JSON válido
4. Headers CORS sendo filtrados corretamente
5. Backend retornando 200 OK

### ❌ O que está falhando:
**Angular HttpClient não consegue fazer parsing da resposta**

### Possíveis Causas:

1. **Content-Encoding**: A resposta original tem `Content-Encoding: gzip` mas estamos removendo esse header. O corpo pode estar comprimido mas o Angular espera texto plano.

2. **Transfer-Encoding: chunked**: Removemos o header mas o corpo pode ainda estar em formato chunked.

3. **Charset**: O Content-Type pode não ter charset especificado.

4. **BOM (Byte Order Mark)**: O corpo pode ter caracteres invisíveis no início.

### Solução Proposta:

**Opção 1**: Fazer parse do JSON no backend e re-serializar
- Garantir que o JSON está limpo
- Remover qualquer encoding problemático

**Opção 2**: Usar `responseType: 'text'` no frontend e fazer parse manual
- Evitar o parser automático do Angular
- Fazer parse manual do JSON

**Opção 3**: Configurar RestTemplate para não usar encoding
- Desabilitar gzip/compression no RestTemplate
- Garantir que a resposta seja texto plano

## Próximos Passos:

Vou implementar a **Opção 1** - fazer parse e re-serialização no backend para garantir JSON limpo.
