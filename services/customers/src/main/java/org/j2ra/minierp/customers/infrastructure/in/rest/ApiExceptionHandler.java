package org.j2ra.minierp.customers.infrastructure.in.rest;

import lombok.extern.slf4j.Slf4j;
import org.j2ra.minierp.customers.domain.AddressNotFoundException;
import org.j2ra.minierp.customers.domain.ContactNotFoundException;
import org.j2ra.minierp.customers.domain.CustomerInactiveException;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.j2ra.minierp.customers.domain.CustomerVersionConflictException;
import org.j2ra.minierp.customers.domain.DuplicateVatNumberException;
import org.j2ra.minierp.customers.domain.InvalidCustomerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Inbound adapter: turns errors into Problem Details (RFC 9457). Stack traces go to the log only.
 * One handler per business exception. The messages are written here and never contain the rejected value.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** A body that fails Bean Validation: 400 with the list of bad fields. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(exception, invalidFields(errors), headers, status, request);
    }

    @ExceptionHandler(InvalidCustomerException.class)
    ProblemDetail handleInvalidCustomer(InvalidCustomerException exception) {
        return invalidFields(List.of(Map.of("field", exception.field(), "message", exception.reason())));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    ProblemDetail handleNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Customer not found.");
    }

    @ExceptionHandler(ContactNotFoundException.class)
    ProblemDetail handleContactNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Contact not found.");
    }

    @ExceptionHandler(AddressNotFoundException.class)
    ProblemDetail handleAddressNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Address not found.");
    }

    @ExceptionHandler(DuplicateVatNumberException.class)
    ProblemDetail handleDuplicateVatNumber() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A customer with this VAT number already exists.");
    }

    @ExceptionHandler(CustomerVersionConflictException.class)
    ProblemDetail handleVersionConflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The customer was changed by someone else. Read it again and retry.");
    }

    @ExceptionHandler(CustomerInactiveException.class)
    ProblemDetail handleInactive() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The customer is inactive. Reactivate it first.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) throws Exception {
        // Security errors belong to the security filters (they answer 401 or 403).
        if (exception instanceof AccessDeniedException || exception instanceof AuthenticationException) {
            throw exception;
        }
        // The stack trace goes to the log only, never to the client.
        log.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private static ProblemDetail invalidFields(List<Map<String, String>> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields are not valid.");
        problem.setProperty("errors", errors);
        return problem;
    }
}
