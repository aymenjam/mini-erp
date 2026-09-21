package org.j2ra.minierp.archfixture.r11.infrastructure.in.rest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** Wrong sample: every endpoint method is missing @Operation. */
@RestController
public class BadController {

    @GetMapping("/get")
    public String get() {
        return "";
    }

    @PostMapping("/post")
    public String post() {
        return "";
    }

    @PutMapping("/put")
    public String put() {
        return "";
    }

    @PatchMapping("/patch")
    public String patch() {
        return "";
    }

    @DeleteMapping("/delete")
    public void delete() {
    }
}
