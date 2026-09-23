package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
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
                .andExpect(jsonPath("$.info.title").value("customers"))
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

    @Test
    @DisplayName("AC-15: the customer list is documented: summary, 200, 400 (problem+json), and the DTO schemas with descriptions")
    void customer_list_is_documented() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/customers'].get.summary").value("List customers"))
                .andExpect(jsonPath("$.paths['/api/v1/customers'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customers'].get.responses['400'].content['application/problem+json']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customers'].get.responses['401'].$ref").value("#/components/responses/Unauthorized"))
                .andExpect(jsonPath("$.paths['/api/v1/customers'].get.responses['403'].$ref").value("#/components/responses/Forbidden"))
                .andExpect(jsonPath("$.paths['/api/v1/customers'].get.parameters[?(@.name=='size')].schema.maximum").value(100))
                .andExpect(jsonPath("$.components.schemas.CustomerPageResponse.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.CustomerPageResponse.properties.totalElements.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.CustomerResponse.description").isNotEmpty());
    }
    @Test
    @DisplayName("AC-12: create, get, update, deactivate and reactivate are documented with their success and business errors")
    void customer_endpoints_are_documented() throws Exception {
        String post = "$.paths['/api/v1/customers'].post";
        String one = "$.paths['/api/v1/customers/{id}']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(post + ".summary").value("Create a customer"))
                .andExpect(jsonPath(post + ".responses['201']").exists())
                .andExpect(jsonPath(post + ".responses['400'].content['application/problem+json']").exists())
                .andExpect(jsonPath(post + ".responses['409'].content['application/problem+json']").exists())
                .andExpect(jsonPath(one + ".get.summary").value("Get one customer"))
                .andExpect(jsonPath(one + ".get.responses['404'].content['application/problem+json']").exists())
                .andExpect(jsonPath(one + ".put.summary").value("Update a customer"))
                .andExpect(jsonPath(one + ".put.responses['200']").exists())
                .andExpect(jsonPath(one + ".put.responses['400']").exists())
                .andExpect(jsonPath(one + ".put.responses['404']").exists())
                .andExpect(jsonPath(one + ".put.responses['409'].content['application/problem+json']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customers/{id}/deactivate'].post.summary").value("Deactivate a customer"))
                .andExpect(jsonPath("$.paths['/api/v1/customers/{id}/deactivate'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customers/{id}/reactivate'].post.summary").value("Reactivate a customer"))
                .andExpect(jsonPath("$.paths['/api/v1/customers/{id}/reactivate'].post.responses['404']").exists())
                // There is no way to delete a customer.
                .andExpect(jsonPath(one + ".delete").doesNotExist());
    }

    @Test
    @DisplayName("AC-12: the customer DTOs have a description, and their fields have descriptions and examples")
    void customer_schemas_are_described() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.CreateCustomerRequest.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.CreateCustomerRequest.properties.name.example").value("ACME Ltd"))
                .andExpect(jsonPath("$.components.schemas.CreateCustomerRequest.properties.vatNumber.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.UpdateCustomerRequest.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.UpdateCustomerRequest.properties.version.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.CustomerResponse.properties.version.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.CustomerResponse.properties.status.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.CustomerSummaryResponse.description").isNotEmpty());
    }
}
