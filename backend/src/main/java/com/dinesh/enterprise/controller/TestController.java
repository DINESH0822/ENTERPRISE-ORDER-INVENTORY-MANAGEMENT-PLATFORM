package com.dinesh.enterprise.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple endpoints used for verifying role‑based access control during Phase 3.
 * These endpoints do not contain any business logic; they merely return a plain
 * message indicating successful access. The security rules are defined in
 * {@link com.dinesh.enterprise.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/customer")
    public ResponseEntity<String> customerEndpoint() {
        return ResponseEntity.ok("Customer endpoint accessed");
    }

    @GetMapping("/admin")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Admin endpoint accessed");
    }
}
