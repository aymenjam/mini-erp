package org.j2ra.minierp.template;

import org.j2ra.minierp.template.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AC-20: with the profile dev, the OpenAPI document and Swagger UI are open, and the document has our security scheme and errors. */
@ActiveProfiles("dev")
class ApiDocsDevProfileTest extends AbstractPostgresIntegrationTest {

    @Test
    @DisplayName("AC-20: /v3/api-docs is open, valid OpenAPI 3, titled with the application name")
    void api_docs_are_open_and_valid() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("service-template"))
                // The test controller of the support package is a real endpoint for springdoc, so paths is not empty.
                .andExpect(jsonPath("$.paths['/test/secured']").exists());
    }

    @Test
    @DisplayName("AC-20: the document has the Bearer JWT scheme and applies it to all endpoints")
    void api_docs_have_bearer_scheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0]['bearer-jwt']").exists());
    }

    @Test
    @DisplayName("AC-20: 401, 403 and 500 are reusable responses in application/problem+json, and every operation refers to them")
    void api_docs_have_problem_responses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.responses.Unauthorized.content['application/problem+json']").exists())
                .andExpect(jsonPath("$.components.responses.Forbidden.content['application/problem+json']").exists())
                .andExpect(jsonPath("$.components.responses.InternalServerError.content['application/problem+json']").exists())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.status").exists())
                .andExpect(jsonPath("$.paths['/test/secured'].get.responses['401'].$ref").value("#/components/responses/Unauthorized"))
                .andExpect(jsonPath("$.paths['/test/secured'].get.responses['403'].$ref").value("#/components/responses/Forbidden"))
                .andExpect(jsonPath("$.paths['/test/secured'].get.responses['500'].$ref").value("#/components/responses/InternalServerError"));
    }

    @Test
    @DisplayName("AC-20: Swagger UI is open without a token")
    void swagger_ui_is_open() throws Exception {
        // /swagger-ui.html redirects to the UI page. Both must pass security. A 401 here would mean the page is closed.
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
