package com.api_portal.backend.modules.auth.config;

import com.api_portal.backend.modules.auth.filter.ApiKeyAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final ApiKeyAuthFilter apiKeyAuthFilter;
    
    @Value("${keycloak.url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm:apicv}")
    private String realm;
    
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
            .requestMatchers(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**"
            );
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/health",
                    "/api/v1/auth/oauth2/**",
                    "/api/v1/auth/verify-email",
                    "/actuator/**",
                    "/error",
                    "/gateway/api/**",
                    "/api/v1/gateway/test"
                ).permitAll()
                // Endpoints públicos de leitura (GET)
                .requestMatchers(
                    org.springframework.http.HttpMethod.GET,
                    "/api/v1/categories",
                    "/api/v1/categories/**",
                    "/api/v1/apis",
                    "/api/v1/apis/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permitir origens do frontend
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000",
            "http://127.0.0.1:4200"
        ));
        
        // Permitir todos os métodos HTTP
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Permitir todos os headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Permitir credenciais (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Headers expostos para o frontend
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"
        ));
        
        // Tempo de cache para preflight requests (1 hora)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configurado para permitir origens: {}", configuration.getAllowedOrigins());
        
        return source;
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            String jwkSetUri = String.format("%s/realms/%s/protocol/openid-connect/certs", 
                keycloakUrl, realm);
            log.info("Configurando JwtDecoder com URI: {}", jwkSetUri);
            log.info("Keycloak URL: {}, Realm: {}", keycloakUrl, realm);
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } catch (Exception e) {
            log.error("Erro ao configurar JwtDecoder: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao configurar JwtDecoder", e);
        }
    }
}
