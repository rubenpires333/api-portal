# Configuração de Secrets

Este projeto usa `application-local.properties` para armazenar secrets localmente.

## Setup Inicial

1. Copie o arquivo de exemplo:
```bash
cp application-local.properties.example application-local.properties
```

2. Edite `application-local.properties` e adicione suas chaves reais do Stripe:
   - Obtenha as chaves em: https://dashboard.stripe.com/apikeys
   - `billing.stripe.api-key` - Chave secreta (sk_test_...)
   - `billing.stripe.publishable-key` - Chave pública (pk_test_...)
   - `billing.stripe.webhook-secret` - Secret do webhook (whsec_...)

## Importante

- O arquivo `application-local.properties` está no `.gitignore` e NÃO será commitado
- Nunca commite secrets nos arquivos `application.properties` ou `application-billing.properties`
- Cada desenvolvedor deve ter seu próprio `application-local.properties`
