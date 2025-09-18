package com.ead.gearup.controller;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.response.JwtTokensDTO;
import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.service.UserService;
import com.ead.gearup.service.auth.JwtService;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class UserController {

    private final JwtService jwtService;
    private final UserService userService;
        
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createUser(
            @Valid @RequestBody UserCreateDTO userCreateDTO,
            HttpServletRequest request) {

        UserResponseDTO createdUser = userService.createUser(userCreateDTO);

        ApiResponseDTO<UserResponseDTO> apiResponse = ApiResponseDTO.<UserResponseDTO>builder()
                .status("success")
                .message("User created successfully!")
                .data(createdUser)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/verify-email")
    public RedirectView verifyEmail(@RequestParam("otp") String otp) {
        String result = userService.validateEmailVerificationToken(otp);
        if ("valid".equals(result)) {
                return new RedirectView("/success.html");
        } else {
                return new RedirectView("/error.html");
        }
    }
    
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> verifyUser(
            @Valid @RequestBody UserLoginDTO userLoginDTO,
            HttpServletRequest request) {

        JwtTokensDTO tokens = userService.verifyUser(userLoginDTO);

        // HttpOnly refresh token cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(jwtService.getRefreshTokenDurationMs()))
                .sameSite("None")
                .build();

        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setAccessToken(tokens.getAccessToken());

        ApiResponseDTO<LoginResponseDTO> apiResponse = ApiResponseDTO.<LoginResponseDTO>builder()
                .status("success")
                .message("Login successful")
                .data(loginResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResponseDTO loginResponse = userService.getRefreshAccessToken(refreshToken);

        ApiResponseDTO<LoginResponseDTO> apiResponse = ApiResponseDTO.<LoginResponseDTO>builder()
                .status("success")
                .message("Token refreshed successfully")
                .data(loginResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }
    


    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Object>> logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletRequest request) {

        if (refreshToken == null || refreshToken.isEmpty()) {
                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                        .status("error")
                        .message("No active session found or already logged out")
                        .data(null)
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Clear the refresh token cookie
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("None")
                .build();

        ApiResponseDTO<Object> apiResponse = ApiResponseDTO.builder()
                .status("success")
                .message("Logged out successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(apiResponse);
    }
}
