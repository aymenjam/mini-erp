# API guide

How to build and document an API in a service. Decision and comparison: [ADR-009](adr/009-api-styles.md).
Naming, pagination and time rules: `constitution.md`, section 4. The customer classes below are **examples**. They are not in the template.

## 1. REST or GraphQL?
- **REST is the default.** Use it for commands (create, change, delete), calls between services and simple lists.
- **GraphQL is optional.** Think about it only for an aggregated read screen of the Angular frontend, when REST needs many calls.
  Write a short ADR first (see ADR-009). The template has no GraphQL library and no schema.
- Both are inbound adapters. They call input ports only and hold no business rule (rules R3, R10).

## 2. Versioning: `/api/v1`
- Every path starts with `/api/v1/<resource>`: plural nouns, kebab-case (`/api/v1/purchase-orders`).
- A compatible change (a new optional field, a new endpoint) stays in `v1`. Clients ignore fields they do not know.
- A breaking change (a field removed or renamed, other meaning) needs `/api/v2`. Keep `v1` running until the clients have moved.

## 3. How to document an endpoint
Every endpoint method needs `@Operation`. The build fails without it (rule R11). The document is generated from the code.
Write the success answer and the business errors. 401, 403 and 500 are added by `OpenApiConfig`, so do not repeat them.

```java
// example, not in the template
@RestController
@RequestMapping("/api/v1/customers")
class CustomerController {

    private final GetCustomerUseCase getCustomer;

    CustomerController(GetCustomerUseCase getCustomer) {
        this.getCustomer = getCustomer;
    }

    @Operation(summary = "Get one customer", description = "Needs the role SALES.")
    @ApiResponse(responseCode = "200", description = "The customer")
    @ApiResponse(responseCode = "404", description = "No customer with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SALES')")
    CustomerResponse get(@PathVariable UUID id) {
        return CustomerResponse.from(getCustomer.get(id));
    }
}

// example, not in the template
@Schema(description = "A customer as the API shows it")
record CustomerResponse(
        @Schema(description = "Id of the customer") UUID id,
        @Schema(description = "Legal name", example = "ACME Ltd") String name) {
}
```
- **Request and response DTOs** are records in `infrastructure.in.rest`. Never return a domain object or a JPA entity.
- Describe a DTO with `@Schema` (short description, and an `example` when the format is not obvious).
- Use `@ApiResponse` for each business error the endpoint can give (404, 409, 422). Errors use the format of section 5.

## 4. Open Swagger UI locally
Swagger UI and the OpenAPI document exist **only with the profile `dev`**. In every other profile they are off, and the URLs are refused.

1. Start the infrastructure and the service in dev mode (`README.md`, "Service template").
2. Open <http://localhost:8099/swagger-ui.html>. (The port is `SERVER_PORT` in `application-dev.yaml`. The raw document is at `/v3/api-docs`.)
3. Get a token for a dev user (`README.md`, "Keycloak"). The role must match the endpoint:
   ```bash
   curl -s -X POST http://localhost:8080/realms/mini-erp/protocol/openid-connect/token \
     -d grant_type=password -d client_id=erp-test-cli -d username=sales -d password="$DEV_USER_PASSWORD"
   ```
4. Click **Authorize**, paste the `access_token` value (without the word "Bearer"), and try an endpoint.

`SecurityConfig` opens these URLs only when `springdoc.api-docs.enabled` is `true`. Do not open them for other profiles.

## 5. Error format
Every error is `application/problem+json` (RFC 9457). It has no stack trace. The details go to the log.
```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "You do not have the role needed for this request.",
  "instance": "/api/v1/customers"
}
```
| Status | When | Who writes it |
|---|---|---|
| 401 | No token, or a bad or expired token | Security filters (`ProblemDetailsSecurityHandlers`) |
| 403 | Valid token, but not the role for this endpoint | Security filters |
| 4xx business | Not found, conflict, invalid input | `ApiExceptionHandler` (one handler per business exception, added with the first real endpoint) |
| 500 | Unexpected error. The message never reaches the client | `ApiExceptionHandler` |

Find a request in the log with the header `X-Correlation-Id` (it is also in the log lines of the request).
