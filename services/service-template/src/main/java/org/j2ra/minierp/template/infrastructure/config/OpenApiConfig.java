package org.j2ra.minierp.template.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Shape of the OpenAPI document: title, Bearer JWT, and the error answers every endpoint can give.
 * Only used when springdoc is on (profile dev). Endpoints add their own success and business errors, see docs/api-guide.md.
 */
@Configuration
class OpenApiConfig {

    static final String BEARER_SCHEME = "bearer-jwt";

    // Error answers are Problem Details (RFC 9457), never plain JSON.
    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA = "ProblemDetail";

    // Response name -> status code and text. One place, so the components and the customizer cannot differ.
    private static final Map<String, String> ERROR_STATUS = Map.of(
            "Unauthorized", "401",
            "Forbidden", "403",
            "InternalServerError", "500");

    @Bean
    OpenAPI openApi(@Value("${spring.application.name}") String applicationName) {
        return new OpenAPI()
                .info(new Info().title(applicationName).version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSchemas(PROBLEM_SCHEMA, problemDetailSchema())
                        .addResponses("Unauthorized", problemResponse("A valid access token is required."))
                        .addResponses("Forbidden", problemResponse("The token does not have the role needed for this request."))
                        .addResponses("InternalServerError", problemResponse("Unexpected error. The details are in the server log only.")))
                // Every endpoint needs a token, except the ones that say otherwise.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /** Adds 401, 403 and 500 to every operation that does not describe them, so no controller repeats them. */
    @Bean
    OpenApiCustomizer errorResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().stream()
                    .flatMap(path -> path.readOperations().stream())
                    .forEach(operation -> ERROR_STATUS.forEach((name, status) -> {
                        ApiResponses responses = operation.getResponses();
                        if (responses.get(status) == null) {
                            responses.addApiResponse(status, new ApiResponse().$ref("#/components/responses/" + name));
                        }
                    }));
        };
    }

    private static ApiResponse problemResponse(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA))));
    }

    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Unauthorized"))
                .addProperty("status", new IntegerSchema().example(401))
                .addProperty("detail", new StringSchema().example("A valid access token is required."))
                .addProperty("instance", new StringSchema().example("/api/v1/customers"));
    }
}
