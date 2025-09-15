package com.ead.gearup.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.service.UserService;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createUser(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(userCreateDTO));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> getToken(@Valid @RequestBody UserLoginDTO userLoginDTO,
            HttpServletRequest request) {

        LoginResponseDTO loginResponse = userService.verifyUser(userLoginDTO);

        ApiResponseDTO<LoginResponseDTO> apiResponse = ApiResponseDTO.<LoginResponseDTO>builder()
                .status("success")
                .message("Login successful")
                .data(loginResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
