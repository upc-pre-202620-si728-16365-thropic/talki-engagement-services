package com.thropic.talki.progress.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI 3 para el progress-service.
 *
 * Publica la especificación en /v3/api-docs y la UI en /swagger-ui.html.
 * La URL relativa permite usar "Try it out" en cualquier puerto; la URL del
 * ambiente publicado se agrega cuando exista.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI progressServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Talki — Progress Service API")
                        .description("Microservicio del bounded context Progress & Adaptation del producto Talki. "
                                + "Consume scoring.completed del exchange talki.events y actualiza el resumen del "
                                + "estudiante, una sola vez por sesión. GET /v1/progress/dashboard consulta ese "
                                + "resumen para el cliente web.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Thropic — Equipo Talki")
                                .url("https://github.com/upc-pre-202620-si728-16365-thropic"))
                        .license(new License()
                                .name("Académico — UPC 1ASI0728 2026-20")))
                .servers(List.of(
                        new Server().url("/").description("Servidor actual"),
                        new Server().url("http://localhost:8089").description("Local, puerto por defecto")
                ));
    }
}
