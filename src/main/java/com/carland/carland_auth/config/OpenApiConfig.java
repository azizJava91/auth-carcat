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
import org.springdoc.core.models.GroupedOpenApi;
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
                **auth-service** — identity microservice.

                ### Two contracts (temporary dual period)
                | Definition | Base path | Audience |
                |---|---|---|
                | **1A. Auth — Legacy** | `/api/v1/users`, `/api/v1/otp` | Current production app |
                | **1B. Auth — NewUsers** | `/api/v1/newUsers` | New PIN / authToken flow (PO) |

                Legacy uses `password` / `registerToken` (Bearer authentication token on OTP/setPassword).
                NewUsers uses body `authToken`, `pinCode`, `deviceToken`+`platform` on loginNew.
                Both store the PIN as BCrypt in DB column `password` (Java field `pin`).
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("1. Auth — Login / Register / Tokens")
                        .description(description)
                        .version("v1")
                        .contact(new Contact().name("Carland Engineering")))
                .servers(List.of(new Server()
                        .url(publicServerUrl)
                        .description("Production via nginx (/auth/server → auth-service)")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access JWT from login (or paste without 'Bearer '). Legacy OTP/setPassword use authentication/register token.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public GroupedOpenApi legacyUsersGroup() {
        return GroupedOpenApi.builder()
                .group("legacy-users")
                .displayName("1A. Auth — Legacy (/users)")
                .pathsToMatch("/api/v1/users/**", "/api/v1/otp/**")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("1A. Auth — Legacy")
                        .description("""
                                **Legacy** contract for the current mobile app.

                                - `POST /api/v1/users/register` (alias: `/authentication`)
                                - OTP: `/api/v1/otp/createAndSend`, `/verify` (Bearer authentication token)
                                - `PUT /api/v1/users/set/password` (alias: `/set/pin`) — body field `password`
                                - `POST /api/v1/users/login` — `password` (+ optional `deviceToken`/`platform`)
                                - Lock: HTTP **429** `LOGIN_LOCKED` after 3 wrong PINs (5 minutes)
                                """))
                .build();
    }

    @Bean
    public GroupedOpenApi newUsersGroup() {
        return GroupedOpenApi.builder()
                .group("new-users")
                .displayName("1B. Auth — NewUsers")
                .pathsToMatch("/api/v1/newUsers/**")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("1B. Auth — NewUsers")
                        .description("""
                                **New** parallel flow (PO). Base: `/api/v1/newUsers`.

                                1. `POST /auth` → `authToken` + `next` (`SEND_OTP` | `PIN_CHECK`)
                                2. `POST /otp/createAndSendNew` `{ authToken }`
                                3. `POST /otp/verifyNew` `{ authToken, otp }` → user created if needed, `next: SET_PIN`
                                4. `PUT /setPinCode?purpose=REGISTER|RESET` `{ authToken, pinCode }` — no access/refresh
                                5. `POST /loginNew` `{ phoneNumber, pinCode, deviceToken, platform }`

                                Forgot PIN: `/auth` with `purpose: RESET` (404 if user missing).
                                """))
                .build();
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
                    param.setDescription("Client locale. Fixed to `az` in Swagger UI (read-only).");
                    param.setRequired(true);
                    param.setSchema(new StringSchema()
                            ._default("az")
                            .example("az")
                            .readOnly(true));
                }
            }
            return operation;
        };
    }
}
