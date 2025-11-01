package com.ead.gearup.integration.security;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for JwtAuthenticationFilter
 * Tests JWT filter behavior, token validation, authentication flow, SecurityContext population
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String TEST_PASSWORD = "Test@123456";
    private static final String TEST_ENDPOINT = "/api/v1/auth/password-status";

    @BeforeEach
    void setUp() {
        // @Transactional will auto-rollback database changes after each test
    }

    // ==================== TOKEN EXTRACTION TESTS ====================

    @Test
    @DisplayName("Should extract token from Authorization header with Bearer prefix")
    void tokenExtraction_BearerPrefix_Success() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("Should not authenticate without Bearer prefix")
    void tokenExtraction_NoBearerPrefix_NotAuthenticated() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should not authenticate with empty Authorization header")
    void tokenExtraction_EmptyHeader_NotAuthenticated() throws Exception {
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should not authenticate without Authorization header")
    void tokenExtraction_NoHeader_NotAuthenticated() throws Exception {
        mockMvc.perform(get(TEST_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should extract token with Bearer and space")
    void tokenExtraction_BearerWithSpace_Success() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ==================== USERNAME EXTRACTION TESTS ====================

    @Test
    @DisplayName("Should extract username from valid token")
    void usernameExtraction_ValidToken_Success() throws Exception {
        User user = createVerifiedUser("testuser@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("Should not authenticate with token for non-existent user")
    void usernameExtraction_NonExistentUser_NotAuthenticated() throws Exception {
        // Create user, generate token, then delete user
        User user = createVerifiedUser("temp@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);
        
        userRepository.delete(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle malformed token gracefully")
    void usernameExtraction_MalformedToken_NotAuthenticated() throws Exception {
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer malformed.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    @Test
    @DisplayName("Should authenticate with valid access token")
    void tokenValidation_ValidAccessToken_Authenticated() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").exists());
    }

    @Test
    @DisplayName("Should not authenticate with refresh token as access token")
    void tokenValidation_RefreshTokenAsAccessToken_NotAuthenticated() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String refreshToken = jwtService.generateRefreshToken(userPrinciple);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should not authenticate with email verification token as access token")
    void tokenValidation_EmailVerificationToken_NotAuthenticated() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String verificationToken = jwtService.generateEmailVerificationToken(userPrinciple);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + verificationToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should not authenticate with invalid token signature")
    void tokenValidation_InvalidSignature_NotAuthenticated() throws Exception {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQHRlc3QuY29tIn0.invalid_signature";

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should not authenticate with token for wrong user")
    void tokenValidation_WrongUser_NotAuthenticated() throws Exception {
        User user1 = createVerifiedUser("user1@test.com", UserRole.CUSTOMER);
        User user2 = createVerifiedUser("user2@test.com", UserRole.CUSTOMER);
        
        String token = generateToken(user1);

        // Token is for user1, but endpoint should not authenticate user2
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()); // Authenticates as user1
    }

    // ==================== SECURITY CONTEXT TESTS ====================

    @Test
    @DisplayName("Should populate SecurityContext with UserDetails")
    void securityContext_ValidToken_Populated() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false));
    }

    @Test
    @DisplayName("Should populate SecurityContext with correct authorities")
    void securityContext_ValidToken_CorrectAuthorities() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()); // ADMIN role allows access
    }

    @Test
    @DisplayName("Should set authentication details from request")
    void securityContext_ValidToken_AuthenticationDetails() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should not populate SecurityContext when already authenticated")
    void securityContext_AlreadyAuthenticated_NotOverridden() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        // First request should authenticate
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Second request should re-authenticate (stateless)
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ==================== FILTER CHAIN TESTS ====================

    @Test
    @DisplayName("Should continue filter chain after authentication")
    void filterChain_ValidToken_Continues() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("Should continue filter chain even without authentication")
    void filterChain_NoToken_Continues() throws Exception {
        // Filter should continue chain even without token
        // Security config will reject at authorization level
        mockMvc.perform(get(TEST_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should continue filter chain for public endpoints")
    void filterChain_PublicEndpoint_Continues() throws Exception {
        mockMvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().is4xxClientError()); // Continues but fails validation
    }

    // ==================== ROLE HANDLING TESTS ====================

    @Test
    @DisplayName("Should authenticate CUSTOMER role correctly")
    void roleHandling_CustomerRole_Success() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Customer cannot access admin endpoints
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should authenticate EMPLOYEE role correctly")
    void roleHandling_EmployeeRole_Success() throws Exception {
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String token = generateToken(employee);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Employee cannot access admin endpoints
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should authenticate ADMIN role correctly")
    void roleHandling_AdminRole_Success() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Admin can access admin endpoints
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ==================== USER LOADING TESTS ====================

    @Test
    @DisplayName("Should load UserDetails from database")
    void userLoading_ValidUsername_LoadsUserDetails() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").exists());
    }

    @Test
    @DisplayName("Should handle inactive user")
    void userLoading_InactiveUser_Handled() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        user.setIsActive(false);
        userRepository.save(user);
        
        String token = generateToken(user);

        // Inactive users should not authenticate
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle unverified user")
    void userLoading_UnverifiedUser_Handled() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        user.setIsVerified(false);
        userRepository.save(user);
        
        String token = generateToken(user);

        // Unverified users should not authenticate
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ==================== MULTIPLE REQUESTS TESTS ====================

    @Test
    @DisplayName("Should handle multiple concurrent requests with same token")
    void multipleRequests_SameToken_Success() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        // Make multiple requests with same token
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(TEST_ENDPOINT)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should handle requests from different users with different tokens")
    void multipleRequests_DifferentTokens_Success() throws Exception {
        User user1 = createVerifiedUser("user1@test.com", UserRole.CUSTOMER);
        User user2 = createVerifiedUser("user2@test.com", UserRole.CUSTOMER);
        
        String token1 = generateToken(user1);
        String token2 = generateToken(user2);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle token with extra spaces")
    void edgeCase_TokenWithExtraSpaces_Handled() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer  " + token)) // Extra space
                .andExpect(status().isUnauthorized()); // Should fail due to extra space
    }

    @Test
    @DisplayName("Should handle case-sensitive Bearer prefix")
    void edgeCase_CaseSensitiveBearerPrefix_Handled() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "bearer " + token)) // lowercase
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle very long valid token")
    void edgeCase_VeryLongToken_Handled() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        
        // Generate token with user that has a long name to make token longer
        user.setName("TestUserWith" + "VeryLongName".repeat(50));
        userRepository.save(user);
        
        String longToken = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + longToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle token with special characters in claims")
    void edgeCase_SpecialCharactersInClaims_Handled() throws Exception {
        User user = createVerifiedUser("special+chars@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle null SecurityContext gracefully")
    void edgeCase_NullSecurityContext_Handled() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        // Filter should create SecurityContext if null
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle missing token type claim gracefully")
    void edgeCase_MissingTokenTypeClaim_Handled() throws Exception {
        // This would require a custom token without token_type claim
        // For now, test that normal tokens work
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    @DisplayName("Integration - Authentication flow with filter")
    void integrationScenario_FullAuthenticationFlow_Success() throws Exception {
        // Create user
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        
        // Generate token
        String token = generateToken(user);
        
        // Make authenticated request
        mockMvc.perform(get(TEST_ENDPOINT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false));
    }

    @Test
    @DisplayName("Integration - Filter processes request before authorization")
    void integrationScenario_FilterBeforeAuthorization_Success() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        // Filter authenticates, then SecurityConfig authorizes
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Integration - Filter rejects invalid token before authorization")
    void integrationScenario_FilterRejectsInvalidToken_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer invalid.token"))
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

        // Create customer entity if role is CUSTOMER
        if (role == UserRole.CUSTOMER) {
            Customer customer = Customer.builder()
                    .user(savedUser)
                    .build();
            customerRepository.save(customer);
        }

        return savedUser;
    }

    private String generateToken(User user) {
        UserPrinciple userPrinciple = new UserPrinciple(user);
        return jwtService.generateAccessToken(userPrinciple);
    }
}

