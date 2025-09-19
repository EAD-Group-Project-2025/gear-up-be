package com.ead.gearup.exception;

import com.ead.gearup.dto.response.ApiResponseDTO;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        // Handle validation errors (@Valid)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleValidationException(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                String errorMessages = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(errorMessages)
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

        // Handle authentication errors
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleBadCredentials(
                        BadCredentialsException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message("Invalid credentials")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Handle generic exceptions
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleException(
                        Exception ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // Handle email already exists exception
        @ExceptionHandler(EmailAlreadyExistsException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleEmailAlreadyExists(
                        EmailAlreadyExistsException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Handle invalid refresh token
        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleInvalidRefreshToken(
                        InvalidRefreshTokenException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Handle user not found exception
        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleUserNotFound(
                        UserNotFoundException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Handle email not varified exception
        @ExceptionHandler(EmailNotVerifiedException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleEmailNotVerified(
                        EmailNotVerifiedException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // Handle email not sending exception
        @ExceptionHandler(EmailSendingException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleEmailSending(
                        EmailSendingException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // Handle email not sending exception
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleAccessDenied(
                        AccessDeniedException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Handle database integrity violations (e.g., duplicate email)
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message("Database constraint violation: " + ex.getMostSpecificCause().getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Handle expired JWT token
        @ExceptionHandler(ExpiredJwtException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleExpiredJwt(
                        ExpiredJwtException ex,
                        HttpServletRequest request) {

                ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                                .status("error")
                                .message("JWT token has expired")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .data(null)
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
}
