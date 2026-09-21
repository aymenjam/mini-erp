package org.j2ra.minierp.archfixture.r11.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Right sample: the rule must not report it. A method without a mapping annotation needs no @Operation. */
@RestController
public class GoodController {

    @Operation(summary = "Documented")
    @GetMapping("/documented")
    public String documented() {
        return "";
    }

    String helper() {
        return "";
    }
}
