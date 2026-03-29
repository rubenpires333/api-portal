package com.api_portal.backend.modules.api.domain.enums;

public enum AuthType {
    NONE,           // Sem autenticação
    API_KEY,        // Autenticação via API Key
    OAUTH2,         // OAuth 2.0
    BASIC,          // Basic Authentication
    BEARER,          // Bearer Token
    JWT
}
