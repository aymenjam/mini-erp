package org.j2ra.minierp.template.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Test endpoints (test sources only). The template has no real endpoint. */
@RestController
@RequestMapping("/test")
public class TestSecuredController {

    private static final Logger log = LoggerFactory.getLogger(TestSecuredController.class);

    /** Needs role SALES. Returns the authorities, so a test can see ROLE_SALES. */
    @GetMapping("/secured")
    @PreAuthorize("hasRole('SALES')")
    public List<String> secured(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    }

    /** Fails with a message that must never reach the client. */
    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("secret internal detail");
    }

    /** Writes a log line and returns the correlation id that the code sees. */
    @GetMapping("/correlation")
    public String correlation() {
        log.info("correlation test endpoint called");
        return String.valueOf(MDC.get("correlationId"));
    }
}
