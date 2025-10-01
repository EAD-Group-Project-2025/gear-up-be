package com.ead.gearup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/health-check")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Health Check API", description = "Endpoints to verify the availability and status of the application")
public class HealthCheckController {

    @GetMapping
    // @RequiresRole({ UserRole.CUSTOMER })
    public ResponseEntity<String> healthCheck() {
        String message = "gearup-backend is running successfully";
        return ResponseEntity.status(HttpStatus.OK).body(message);
    }
}
