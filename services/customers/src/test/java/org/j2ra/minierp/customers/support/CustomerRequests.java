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

    /** Creates a customer as SALES and returns the id from the answer. */
    public static String createCompany(MockMvc mockMvc, String name, String vatNumber) throws Exception {
        MvcResult result = mockMvc.perform(json(post(URL), company(name, vatNumber), "SALES")).andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + 6;
        return body.substring(start, body.indexOf('"', start));
    }
}
