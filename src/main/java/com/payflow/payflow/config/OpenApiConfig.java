package com.payflow.payflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PayFlow API",
                version = "1.0",
                description = "A distributed payment & transaction processing system — practice project " +
                        "implementing wallets, P2P transfers, idempotency, fraud checks, event-driven " +
                        "processing, and audit logging.",
                contact = @Contact(name = "PayFlow", email = "dev@payflow.local")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}