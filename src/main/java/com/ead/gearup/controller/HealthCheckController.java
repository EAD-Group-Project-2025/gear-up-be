package com.ead.gearup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.validation.RequiresRole;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/public/v1/health-check")
@SecurityRequirement(name = "bearerAuth")
public class HealthCheckController {

    @GetMapping
    @RequiresRole({ UserRole.CUSTOMER })
    public ResponseEntity<String> healthCheck() {
        String message = "voluntrix-backend is running successfully";
        return ResponseEntity.status(HttpStatus.OK).body(message);
    }
}
