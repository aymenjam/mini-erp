package org.j2ra.minierp.customers.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Small helpers for the customer API tests. The bodies are plain JSON text, so the tests show the real wire format. */
public final class CustomerRequests {

    public static final String URL = "/api/v1/customers";

    private CustomerRequests() {
    }

    public static String bearer(String... roles) {
        return "Bearer " + TestTokens.withRoles(roles);
    }

    public static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body, String... roles) {
        return builder.header(HttpHeaders.AUTHORIZATION, bearer(roles)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    public static String company(String name, String vatNumber) {
        String vat = vatNumber == null ? "null" : "\"" + vatNumber + "\"";
        return """
                {"type":"COMPANY","name":"%s","email":"contact@acme.example","phone":"+33 1 23 45 67 89","vatNumber":%s}
                """.formatted(name, vat);
    }

    public static String contact(String firstName, String lastName, boolean primary) {
        return """
                {"firstName":"%s","lastName":"%s","email":"%s@acme.example","jobTitle":"Manager","primary":%s}
                """.formatted(firstName, lastName, firstName.toLowerCase(), primary);
    }

    public static String address(String type, String city, String country, boolean defaultForType) {
        return """
                {"type":"%s","line1":"1 rue de la Paix","postalCode":"75002","city":"%s","country":"%s","defaultForType":%s}
                """.formatted(type, city, country, defaultForType);
    }

    /** Reads one value of a JSON answer, for example {@code read(body, "$.contacts[0].id")}. */
    public static <T> T read(String json, String path) {
        return com.jayway.jsonpath.JsonPath.read(json, path);
    }

    /** Creates a customer as SALES and returns the id from the answer. */
    public static String createCompany(MockMvc mockMvc, String name, String vatNumber) throws Exception {
        MvcResult result = mockMvc.perform(json(post(URL), company(name, vatNumber), "SALES")).andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + 6;
        return body.substring(start, body.indexOf('"', start));
    }
}
