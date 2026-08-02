package com.carland.carland_auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Value("${carland.swagger.public-server-url:https://digital-innovation.agency/auth/server}")
    private String publicServerUrl;

    @Bean
    public OpenAPI carlandAuthOpenAPI() {
        String description = """
                Internal API documentation for **carland_auth** (register, OTP, login, refresh, invite).

                ### Login in Swagger
                Use the black **Carland Login** bar: it calls `POST /api/v1/users/login`, stores tokens,
                and auto-fills **Authorize → bearerAuth**. On 401 or near expiry it calls
                `POST /api/v1/users/refresh` with the refresh token.

                ### Defaults
                - `Accept-Language` = `az`

                Prefer the unified UI at `/carland-docs` on carland_service when available.
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("Carland Auth API")
                        .description(description)
                        .version("v1")
                        .contact(new Contact().name("Carland Engineering")))
                .servers(List.of(new Server()
                        .url(publicServerUrl)
                        .description("Production via nginx (/auth/server → this app)")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Filled by Carland Login, or paste access JWT without 'Bearer '.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public OperationCustomizer headerDefaultsCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() == null) {
                return operation;
            }
            operation.getParameters().removeIf(param ->
                    "header".equalsIgnoreCase(param.getIn())
                            && "Authorization".equalsIgnoreCase(param.getName()));

            for (Parameter param : operation.getParameters()) {
                if (!"header".equalsIgnoreCase(param.getIn()) || param.getName() == null) {
                    continue;
                }
                if ("Accept-Language".equalsIgnoreCase(param.getName())) {
                    param.setDescription("Client locale for localized messages.");
                    param.setSchema(new StringSchema()._default("az").example("az"));
                }
            }
            return operation;
        };
    }
}
