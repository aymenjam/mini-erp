/**
 * REST adapter: controllers, request filters and the error handler.
 *
 * <p><b>Role:</b> read the request, call one input port, and write the response (JSON, {@code application/problem+json} for errors).
 * <p><b>Belongs here:</b> controllers, request and response DTOs, mapping from and to domain types, servlet filters, {@code @RestControllerAdvice}.
 * <p>Every endpoint method has {@code @Operation}, so the API documentation is never empty (rule R11). How to document: {@code docs/api-guide.md}.
 * <p><b>Must not be here:</b> business rules, transactions, database access. Controllers live only in this package (rule R8).
 */
package org.j2ra.minierp.template.infrastructure.in.rest;
