package com.mineops.mineopsapi.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publica la documentación interactiva de la API, incluyendo el esquema de autenticación por bearer
 * para poder ejercitar los endpoints desde el navegador después de iniciar sesión.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI mineOpsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MineOps API")
                        .description("""
                                Control de equipos mineros, certificaciones de operadores, asignación a \
                                turnos y mantenimiento por horómetro.
                                """)
                        .version("1.0.0")
                        .license(new License().name("MIT"))
                        .contact(new Contact().name("MineOps")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                        .name(BEARER_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Pega aquí el token que devuelve /api/v1/authentication/sign-in")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}
