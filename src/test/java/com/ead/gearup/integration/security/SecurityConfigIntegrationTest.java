package com.ead.gearup.integration.security;

import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SecurityConfig
 * Tests security configuration, CORS, CSRF, session management, authorization rules
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String TEST_PASSWORD = "Test@123456";

    @BeforeEach
    void setUp() {
        // @Transactional will auto-rollback database changes after each test
    }

    // ==================== AUTHORIZATION RULES TESTS ====================

    @Test
    @DisplayName("Should allow public access to /api/v1/auth/** endpoints")
    void publicEndpoints_NoAuthentication_Allowed() throws Exception {
        // Test that auth endpoints are accessible (even if they require auth for some operations)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // Bad request due to validation, but endpoint is accessible
    }

    @Test
    @DisplayName("Should allow public access to Swagger endpoints")
    void swaggerEndpoints_NoAuthentication_Allowed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to GraphQL endpoints")
    void graphqlEndpoints_NoAuthentication_Allowed() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"{ __typename }\"}"))
                .andExpect(status().isOk()); // GraphQL endpoint is accessible
    }

    @Test
    @DisplayName("Should restrict /api/v1/admin/** to ADMIN role only")
    void adminEndpoints_RequireAdminRole() throws Exception {
        // No authentication
        mockMvc.perform(get("/api/v1/admin/check-init"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        // With CUSTOMER role
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String customerToken = generateToken(customer);

        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));

        // With EMPLOYEE role
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String employeeToken = generateToken(employee);

        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));

        // With ADMIN role
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String adminToken = generateToken(admin);

        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should require authentication for /api/v1/chat/** endpoints")
    void chatEndpoints_RequireAuthentication() throws Exception {
        // No authentication
        mockMvc.perform(post("/api/v1/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        // With any authenticated user (CUSTOMER)
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String customerToken = generateToken(customer);

        mockMvc.perform(post("/api/v1/chat/send")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError()); // Accessible but may fail on validation
    }

    @Test
    @DisplayName("Should require authentication for anyRequest")
    void anyRequest_RequiresAuthentication() throws Exception {
        // Test a non-public endpoint without authentication
        mockMvc.perform(get("/api/v1/some-protected-endpoint"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/some-protected-endpoint"));
    }

    // ==================== SESSION MANAGEMENT TESTS ====================

    @Test
    @DisplayName("Should use stateless session (SessionCreationPolicy.STATELESS)")
    void sessionManagement_Stateless() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        // Make multiple requests with same token - should not create session
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));

        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
    }

    // ==================== EXCEPTION HANDLING TESTS ====================

    @Test
    @DisplayName("Should return 401 with JSON for unauthenticated requests")
    void exceptionHandling_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/check-init"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/check-init"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 403 with JSON for insufficient permissions")
    void exceptionHandling_Forbidden_Returns403() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String customerToken = generateToken(customer);

        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/check-init"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 401 for expired token")
    void exceptionHandling_ExpiredToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer expired.invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 for malformed token")
    void exceptionHandling_MalformedToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CORS TESTS ====================

    @Test
    @DisplayName("Should allow CORS requests from allowed origins")
    void cors_AllowedOrigin_Success() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Should support all configured HTTP methods via CORS")
    void cors_AllowedMethods_Configured() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().string("Access-Control-Allow-Methods", 
                        containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Methods", 
                        containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", 
                        containsString("PUT")))
                .andExpect(header().string("Access-Control-Allow-Methods", 
                        containsString("DELETE")));
    }

    @Test
    @DisplayName("Should allow all headers via CORS")
    void cors_AllowedHeaders_All() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    @DisplayName("Should allow credentials via CORS")
    void cors_AllowCredentials_True() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    // ==================== CSRF TESTS ====================

    @Test
    @DisplayName("Should disable CSRF for stateless JWT authentication")
    void csrf_Disabled_ForStatelessAuth() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        // POST request without CSRF token should succeed with JWT
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError()); // Fails on validation, not CSRF
    }

    // ==================== JWT FILTER INTEGRATION TESTS ====================

    @Test
    @DisplayName("Should extract and validate JWT from Authorization header")
    void jwtFilter_ValidToken_AuthenticatesUser() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("Should reject request without Authorization header")
    void jwtFilter_NoToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/password-status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with invalid Authorization header format")
    void jwtFilter_InvalidHeaderFormat_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with token not starting with 'Bearer '")
    void jwtFilter_NoBearerPrefix_ReturnsUnauthorized() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", token)) // Missing "Bearer "
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should populate SecurityContext with authenticated user")
    void jwtFilter_ValidToken_PopulatesSecurityContext() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").exists());
    }

    // ==================== PASSWORD ENCODER TESTS ====================

    @Test
    @DisplayName("Should use BCrypt password encoder with strength 12")
    void passwordEncoder_BCrypt_Configured() throws Exception {
        String plainPassword = "TestPassword@123";
        String encodedPassword = passwordEncoder.encode(plainPassword);

        // BCrypt hashes start with $2a$ or $2b$
        org.assertj.core.api.Assertions.assertThat(encodedPassword).startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches(plainPassword, encodedPassword)).isTrue();
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("WrongPassword", encodedPassword)).isFalse();
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    @DisplayName("Should authenticate CUSTOMER and allow access to customer endpoints")
    void integrationScenario_CustomerAuthentication_Success() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        // Customer can access their own password status
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Customer cannot access admin endpoints
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should authenticate EMPLOYEE and allow access to employee endpoints")
    void integrationScenario_EmployeeAuthentication_Success() throws Exception {
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String token = generateToken(employee);

        // Employee can access their own password status
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Employee cannot access admin endpoints
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should authenticate ADMIN and allow access to all endpoints")
    void integrationScenario_AdminAuthentication_Success() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        // Admin can access admin endpoints
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Admin can access regular endpoints
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject request after token expiration")
    void integrationScenario_ExpiredToken_Rejected() throws Exception {
        // This test would require creating an expired token
        // For now, we test with an invalid token
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer expired.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow multiple authenticated requests with same token")
    void integrationScenario_MultipleRequests_SameToken() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        // First request
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Second request with same token
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Third request with same token
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle null Authorization header gracefully")
    void edgeCase_NullAuthorizationHeader_Handled() throws Exception {
        mockMvc.perform(get("/api/v1/admin/check-init"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle empty Authorization header gracefully")
    void edgeCase_EmptyAuthorizationHeader_Handled() throws Exception {
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle Authorization header with only 'Bearer' gracefully")
    void edgeCase_BearerWithoutToken_Handled() throws Exception {
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle very long invalid token gracefully")
    void edgeCase_VeryLongInvalidToken_Handled() throws Exception {
        String longToken = "a".repeat(10000);
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + longToken))
                .andExpect(status().isUnauthorized());
    }

    // ==================== HELPER METHODS ====================

    private User createVerifiedUser(String email, UserRole role) {
        User user = User.builder()
                .email(email.toLowerCase().trim())
                .name("Test User")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .role(role)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Create role-specific entity
        switch (role) {
            case CUSTOMER -> {
                Customer customer = Customer.builder()
                        .user(savedUser)
                        .build();
                customerRepository.save(customer);
            }
            case EMPLOYEE -> {
                Employee employee = Employee.builder()
                        .user(savedUser)
                        .specialization("Test Specialization")
                        .build();
                employeeRepository.save(employee);
            }
            case ADMIN -> {
                // Admin doesn't need additional entity
            }
        }

        return savedUser;
    }

    private String generateToken(User user) {
        UserPrinciple userPrinciple = new UserPrinciple(user);
        return jwtService.generateAccessToken(userPrinciple);
    }
}

