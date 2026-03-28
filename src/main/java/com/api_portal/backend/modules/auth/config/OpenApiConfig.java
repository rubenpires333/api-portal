package com.api_portal.backend.modules.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API Portal - Backend",
        version = "1.0.0",
        description = "API de gestão do portal de APIs com suporte para autenticação JWT e API Keys",
        contact = @Contact(
            name = "API Portal Team",
            email = "support@apicv.cv"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Desenvolvimento"),
        @Server(url = "http://localhost:8080", description = "Docker")
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "Token JWT obtido através do endpoint /api/v1/auth/login"
)
@SecurityScheme(
    name = "API Key",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-API-Key",
    description = "API Key para autenticação de aplicações. Obtenha através do endpoint /api/v1/api-keys"
)
public class OpenApiConfig {
}
