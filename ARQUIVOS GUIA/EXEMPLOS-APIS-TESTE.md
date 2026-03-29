# Exemplos de APIs para Testes

Este documento contém exemplos de APIs públicas reais que você pode usar para testar o sistema API Portal.

> **IMPORTANTE:** No campo "Autenticação", use o valor **NONE** (não JWT). Os valores aceitos são: NONE, API_KEY, OAUTH2, BEARER, BASIC.

---

## 📍 Exemplo 1: API ViaCEP - Consulta de CEP

### Dados para Cadastro:

- **Nome:** API ViaCEP - Consulta de CEP
- **Categoria:** Logística (ou Utilidades)
- **Visibilidade:** Pública
- **URL Base:** `https://viacep.com.br/ws`
- **Autenticação:** Nenhuma
- **Rate Limit:** 300 requisições
- **Período:** Por Minuto
- **Requer Aprovação:** Não

### Descrição Curta:
```
API gratuita para consulta de CEP brasileiro. Retorna endereço completo, bairro, cidade, estado e coordenadas geográficas.
```

### Descrição Completa:
```
A API ViaCEP é um webservice gratuito e de alto desempenho para consultar Códigos de Endereçamento Postal (CEP) do Brasil. Retorna dados como logradouro, bairro, cidade, estado, IBGE, GIA, DDD e Siafi. Suporta formatos JSON e XML. Não requer autenticação e possui alta disponibilidade. Ideal para sistemas de e-commerce, cadastros e logística.
```

### Tags:
```
cep, endereço, brasil, logística, correios
```

### Como Testar:

**Endpoint Original:**
```
GET https://viacep.com.br/ws/01310-100/json
```

**Endpoint via Gateway (após criar):**
```
GET http://localhost:8080/gateway/api/api-viacep-consulta-de-cep/01310-100/json
```

**Resposta Esperada:**
```json
{
  "cep": "01310-100",
  "logradouro": "Avenida Paulista",
  "complemento": "",
  "bairro": "Bela Vista",
  "localidade": "São Paulo",
  "uf": "SP",
  "ibge": "3550308",
  "gia": "1004",
  "ddd": "11",
  "siafi": "7107"
}
```

---

## 💰 Exemplo 2: API AwesomeAPI - Cotações de Moedas

### Dados para Cadastro:

- **Nome:** API AwesomeAPI - Cotações de Moedas
- **Categoria:** Finanças
- **Visibilidade:** Pública
- **URL Base:** `https://economia.awesomeapi.com.br`
- **Autenticação:** Nenhuma
- **Rate Limit:** 500 requisições
- **Período:** Por Minuto
- **Requer Aprovação:** Não

### Descrição Curta:
```
API gratuita para consultar cotações de moedas em tempo real. Suporta mais de 150 moedas incluindo USD, EUR, BTC e outras criptomoedas.
```

### Descrição Completa:
```
A AwesomeAPI oferece cotações de moedas atualizadas em tempo real, incluindo moedas tradicionais (USD, EUR, GBP) e criptomoedas (BTC, ETH). Fornece dados históricos, variação percentual, máxima e mínima do dia. Totalmente gratuita e sem necessidade de cadastro. Perfeita para aplicações financeiras, e-commerce internacional e dashboards econômicos.
```

### Tags:
```
moedas, câmbio, finanças, cotação, dólar, euro
```

### Como Testar:

**Endpoint Original:**
```
GET https://economia.awesomeapi.com.br/last/USD-BRL
```

**Endpoint via Gateway (após criar):**
```
GET http://localhost:8080/gateway/api/api-awesomeapi-cotacoes-de-moedas/last/USD-BRL
```

**Resposta Esperada:**
```json
{
  "USDBRL": {
    "code": "USD",
    "codein": "BRL",
    "name": "Dólar Americano/Real Brasileiro",
    "high": "5.1234",
    "low": "5.0987",
    "varBid": "0.0123",
    "pctChange": "0.24",
    "bid": "5.1100",
    "ask": "5.1150",
    "timestamp": "1234567890",
    "create_date": "2024-03-29 10:30:00"
  }
}
```

---

## 📅 Exemplo 3: API Brasil Feriados Nacionais

### Dados para Cadastro:

- **Nome:** API Brasil Feriados Nacionais
- **Categoria:** Utilidades
- **Visibilidade:** Pública
- **URL Base:** `https://brasilapi.com.br/api`
- **Autenticação:** Nenhuma
- **Rate Limit:** 200 requisições
- **Período:** Por Minuto
- **Requer Aprovação:** Não

### Descrição Curta:
```
API gratuita para consultar feriados nacionais brasileiros por ano. Retorna nome, data e tipo do feriado.
```

### Descrição Completa:
```
API que fornece informações sobre feriados nacionais do Brasil. Permite consultar feriados por ano específico, retornando data, nome, tipo (nacional, estadual, municipal) e se é ponto facultativo. Útil para sistemas de RH, calendários corporativos, agendamento de entregas e planejamento de eventos. Totalmente gratuita e mantida pela comunidade BrasilAPI.
```

### Tags:
```
feriados, brasil, calendário, datas, utilidades
```

### Como Testar:

**Endpoint Original:**
```
GET https://brasilapi.com.br/api/feriados/v1/2024
```

**Endpoint via Gateway (após criar):**
```
GET http://localhost:8080/gateway/api/api-brasil-feriados-nacionais/feriados/v1/2024
```

**Resposta Esperada:**
```json
[
  {
    "date": "2024-01-01",
    "name": "Confraternização Universal",
    "type": "national"
  },
  {
    "date": "2024-02-13",
    "name": "Carnaval",
    "type": "national"
  },
  {
    "date": "2024-03-29",
    "name": "Sexta-feira Santa",
    "type": "national"
  }
]
```

---

## 🌐 Exemplo 4: API de Bancos Brasileiros

### Dados para Cadastro:

- **Nome:** API Brasil Bancos
- **Categoria:** Finanças
- **Visibilidade:** Pública
- **URL Base:** `https://brasilapi.com.br/api`
- **Autenticação:** Nenhuma (use NONE no formulário)
- **Rate Limit:** 300 requisições
- **Período:** Por Minuto
- **Requer Aprovação:** Não

### Descrição Curta:
```
API para consultar informações de bancos brasileiros. Retorna nome, código, ISPB e dados completos de instituições financeiras.
```

### Descrição Completa:
```
API gratuita que fornece informações detalhadas sobre bancos e instituições financeiras do Brasil. Permite buscar por código do banco, ISPB ou listar todos os bancos. Retorna nome completo, nome reduzido, código, ISPB, tipo de instituição e status. Essencial para validação de dados bancários, sistemas de pagamento e aplicações financeiras.
```

### Tags:
```
bancos, finanças, brasil, ispb, instituições-financeiras
```

### Como Testar:

**Endpoint Original:**
```
GET https://brasilapi.com.br/api/banks/v1/001
```

**Endpoint via Gateway (após criar):**
```
GET http://localhost:8080/gateway/api/api-brasil-bancos/banks/v1/001
```

**Resposta Esperada:**
```json
{
  "ispb": "00000000",
  "name": "Banco do Brasil S.A.",
  "code": 1,
  "fullName": "Banco do Brasil S.A."
}
```

---

## 🎲 Exemplo 5: API JSONPlaceholder - Dados Fake

### Dados para Cadastro:

- **Nome:** JSONPlaceholder - API de Testes
- **Categoria:** Desenvolvimento
- **Visibilidade:** Pública
- **URL Base:** `https://jsonplaceholder.typicode.com`
- **Autenticação:** Nenhuma
- **Rate Limit:** 1000 requisições
- **Período:** Por Hora
- **Requer Aprovação:** Não

### Descrição Curta:
```
API fake gratuita para testes e prototipagem. Fornece endpoints REST completos com posts, comentários, usuários, fotos e todos.
```

### Descrição Completa:
```
JSONPlaceholder é uma API REST fake gratuita para testes e prototipagem. Oferece 6 recursos comuns (posts, comments, albums, photos, todos, users) com endpoints completos para GET, POST, PUT, PATCH e DELETE. Ideal para aprender desenvolvimento frontend, testar bibliotecas HTTP, criar demos e protótipos. Não requer autenticação e aceita todas as requisições.
```

### Tags:
```
teste, fake, desenvolvimento, prototipagem, rest
```

### Como Testar:

**Endpoint Original:**
```
GET https://jsonplaceholder.typicode.com/posts/1
```

**Endpoint via Gateway (após criar):**
```
GET http://localhost:8080/gateway/api/jsonplaceholder-api-de-testes/posts/1
```

**Resposta Esperada:**
```json
{
  "userId": 1,
  "id": 1,
  "title": "sunt aut facere repellat provident",
  "body": "quia et suscipit\nsuscipit recusandae..."
}
```

---

## 🚀 Recomendação para Primeiro Teste

**Use o Exemplo 1 (ViaCEP)** porque:

✅ Totalmente gratuita  
✅ Não requer autenticação  
✅ API brasileira e confiável  
✅ Resposta rápida  
✅ Fácil de entender o resultado  
✅ Muito usada em produção  

### Passo a Passo:

1. Acesse `/provider/apis/new`
2. Preencha com os dados do Exemplo 1
3. Clique em "Criar"
4. Após criar, teste no navegador ou Postman:
   ```
   GET http://localhost:8080/gateway/api/api-viacep-consulta-de-cep/01310-100/json
   ```
5. Você deve receber os dados do endereço da Av. Paulista

---

## 📝 Notas Importantes

- O slug é gerado automaticamente pelo backend a partir do nome
- Exemplo: "API ViaCEP - Consulta de CEP" → slug: "api-viacep-consulta-de-cep"
- O gateway adiciona automaticamente o slug na URL
- Todas essas APIs são públicas e gratuitas (exceto OpenWeather que tem limite)
- Você pode testar diretamente as URLs originais antes de cadastrar no sistema

---

## 🔧 Testando o Gateway

Após criar qualquer API, você pode testar usando:

**cURL:**
```bash
curl http://localhost:8080/gateway/api/{slug}/{endpoint}
```

**Postman:**
```
GET http://localhost:8080/gateway/api/{slug}/{endpoint}
```

**Navegador:**
```
http://localhost:8080/gateway/api/{slug}/{endpoint}
```

O gateway irá:
1. Buscar a API pelo slug
2. Verificar se está ativa e publicada
3. Fazer a requisição para a URL base + endpoint
4. Retornar a resposta ao cliente
