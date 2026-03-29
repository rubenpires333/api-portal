package com.api_portal.backend.modules.api.domain.enums;

public enum AuthType {
    NONE,              // Sem autenticação
    API_KEY,           // Autenticação via API Key
    BEARER,            // Bearer Token
    BASIC,             // Basic Authentication
    OAUTH2,            // OAuth 2.0
    JWT,               // JWT Bearer
    DIGEST,            // Digest Authentication
    HAWK,              // Hawk Authentication
    AWS_SIGNATURE,     // AWS Signature
    NTLM,              // NTLM Authentication
    AKAMAI_EDGEGRID,   // Akamai EdgeGrid
    ASAP               // ASAP (Atlassian)
}
