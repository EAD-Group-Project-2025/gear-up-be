package com.ead.gearup.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Management", description = "Admin-specific operations and user management")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/check-init")
    @Operation(
        summary = "Check if admin account exists",
        description = "Returns whether the default admin account has been initialized. Useful for setup checks."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Admin check completed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                        "status": "success",
                        "message": "Admin account exists",
                        "data": {
                            "adminExists": true,
                            "adminEmail": "admin@gearup.com"
                        },
                        "timestamp": "2023-10-15T10:30:00Z",
                        "path": "/api/v1/admin/check-init"
                    }
                    """)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<Object>> checkAdminInit(HttpServletRequest request) {
        boolean adminExists = userRepository.findByEmail("admin@gearup.com").isPresent();

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message(adminExists ? "Admin account exists" : "Admin account not initialized")
                .data(new AdminCheckResponse(adminExists, adminExists ? "admin@gearup.com" : null))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/create-admin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create additional admin account",
        description = "Creates a new admin user. This endpoint can be used to add more admins to the system."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Admin created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                        "status": "success",
                        "message": "Admin user created successfully",
                        "data": {
                            "id": "123e4567-e89b-12d3-a456-426614174000",
                            "firstName": "John",
                            "lastName": "Admin",
                            "email": "john.admin@gearup.com",
                            "role": "ADMIN",
                            "isEmailVerified": true
                        },
                        "timestamp": "2023-10-15T10:30:00Z",
                        "path": "/api/v1/admin/create-admin"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or email already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Requires ADMIN role",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createAdmin(
            @Valid @RequestBody 
            @Parameter(description = "Admin user details", required = true)
            UserCreateDTO userCreateDTO,
            HttpServletRequest request) {

        // Check if user already exists
        if (userRepository.findByEmail(userCreateDTO.getEmail()).isPresent()) {
            ApiResponseDTO<UserResponseDTO> errorResponse = ApiResponseDTO.<UserResponseDTO>builder()
                    .status("error")
                    .message("User with this email already exists")
                    .data(null)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // Create admin user
        User admin = User.builder()
                .email(userCreateDTO.getEmail())
                .name(userCreateDTO.getName())
                .password(passwordEncoder.encode(userCreateDTO.getPassword()))
                .role(UserRole.ADMIN)
                .isVerified(true) // Admins are pre-verified
                .build();

        User savedAdmin = userRepository.save(admin);

        // Create response
        UserResponseDTO userResponse = new UserResponseDTO(
                savedAdmin.getEmail(),
                savedAdmin.getName()
        );

        ApiResponseDTO<UserResponseDTO> response = ApiResponseDTO.<UserResponseDTO>builder()
                .status("success")
                .message("Admin user created successfully")
                .data(userResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Inner class for admin check response
    private record AdminCheckResponse(boolean adminExists, String adminEmail) {}
}
