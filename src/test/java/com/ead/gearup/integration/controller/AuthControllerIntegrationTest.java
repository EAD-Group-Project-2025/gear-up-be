package com.ead.gearup.integration.controller;

import com.ead.gearup.dto.request.ResendEmailRequestDTO;
import com.ead.gearup.dto.user.PasswordChangeRequest;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.EmailService;
import com.ead.gearup.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests all authentication endpoints with Spring context loaded
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private EmailService emailService;

    private static final String BASE_URL = "/api/v1/auth";
    private static final String TEST_EMAIL = "test@gearup.com";
    private static final String TEST_PASSWORD = "Test@123456";
    private static final String TEST_NAME = "Test User";

    @BeforeEach
    void setUp() {
        // Reset email service mock before each test
        // Note: @Transactional will auto-rollback database changes after each test
        reset(emailService);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("POST /register - Should register new user successfully")
    void registerUser_ValidData_Success() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(TEST_EMAIL);
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName(TEST_NAME);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("registered successfully")))
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL.toLowerCase()))
                .andExpect(jsonPath("$.data.name").value(TEST_NAME))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.verified").value(false))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));

        // Verify user was created in database
        User savedUser = userRepository.findByEmail(TEST_EMAIL.toLowerCase()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL.toLowerCase());
        assertThat(savedUser.getName()).isEqualTo(TEST_NAME);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(savedUser.getIsVerified()).isFalse();

        // Verify customer entity was created
        Customer customer = customerRepository.findByUser(savedUser).orElse(null);
        assertThat(customer).isNotNull();

        // Verify email was sent
        verify(emailService, times(1)).sendVerificationEmail(
                eq(TEST_EMAIL.toLowerCase()),
                eq(TEST_NAME),
                anyString()
        );
    }

    @Test
    @DisplayName("POST /register - Should normalize email to lowercase")
    void registerUser_UppercaseEmail_NormalizesToLowercase() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("TEST@GEARUP.COM");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName(TEST_NAME);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("test@gearup.com"));

        // Verify normalized email in database
        User user = userRepository.findByEmail("test@gearup.com").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("test@gearup.com");
    }

    @Test
    @DisplayName("POST /register - Should trim whitespace from email")
    void registerUser_EmailWithWhitespace_TrimsWhitespace() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("  test@gearup.com  ");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName(TEST_NAME);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("test@gearup.com"));
    }

    @Test
    @DisplayName("POST /register - Should return 400 when email already exists")
    void registerUser_EmailExists_ReturnsBadRequest() throws Exception {
        // Arrange - Create existing user
        createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(TEST_EMAIL);
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("Another User");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Email already exists")));
    }

    @Test
    @DisplayName("POST /register - Should return 400 for invalid email")
    void registerUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("invalid-email");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName(TEST_NAME);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("email")));
    }

    @Test
    @DisplayName("POST /register - Should return 400 for weak password")
    void registerUser_WeakPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(TEST_EMAIL);
        createDTO.setPassword("weak");
        createDTO.setName(TEST_NAME);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Password")));
    }

    @Test
    @DisplayName("POST /register - Should return 400 for missing required fields")
    void registerUser_MissingFields_ReturnsBadRequest() throws Exception {
        // Arrange - Empty DTO
        UserCreateDTO createDTO = new UserCreateDTO();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /register - Should encode password before saving")
    void registerUser_ValidData_EncodesPassword() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(TEST_EMAIL);
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName(TEST_NAME);

        // Act
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated());

        // Assert - Password should be encoded, not plain text
        User user = userRepository.findByEmail(TEST_EMAIL).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo(TEST_PASSWORD);
        assertThat(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).isTrue();
    }

    // ==================== EMAIL VERIFICATION TESTS ====================

    @Test
    @DisplayName("GET /verify-email - Should verify email with valid token")
    void verifyEmail_ValidToken_Success() throws Exception {
        // Arrange
        User user = createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String token = jwtService.generateEmailVerificationToken(userPrinciple);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/verify-email")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Email verified successfully!"));

        // Verify user is now verified in database
        User verifiedUser = userRepository.findByEmail(TEST_EMAIL).orElse(null);
        assertThat(verifiedUser).isNotNull();
        assertThat(verifiedUser.getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("GET /verify-email - Should return 400 for expired token")
    void verifyEmail_ExpiredToken_ReturnsBadRequest() throws Exception {
        // Arrange
        User user = createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);
        
        // Create expired token (you may need to adjust JwtTestHelper for this)
        String expiredToken = "expired.jwt.token";

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/verify-email")
                        .param("token", expiredToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired")));
    }

    @Test
    @DisplayName("GET /verify-email - Should return 400 for malformed token")
    void verifyEmail_MalformedToken_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/verify-email")
                        .param("token", "malformed-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("GET /verify-email - Should return success if already verified")
    void verifyEmail_AlreadyVerified_ReturnsSuccess() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String token = jwtService.generateEmailVerificationToken(userPrinciple);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/verify-email")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== RESEND EMAIL TESTS ====================

    @Test
    @DisplayName("POST /resend-email - Should resend verification email successfully")
    void resendEmail_ValidRequest_Success() throws Exception {
        // Arrange
        User user = createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);
        user.setLastVerificationEmailSent(LocalDateTime.now().minusMinutes(10)); // Old timestamp
        userRepository.save(user);

        ResendEmailRequestDTO requestDTO = new ResendEmailRequestDTO();
        requestDTO.setEmail(TEST_EMAIL);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/resend-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("resent successfully")));

        // Verify email was sent again
        verify(emailService, times(1)).sendVerificationEmail(
                eq(TEST_EMAIL),
                eq(TEST_NAME),
                anyString()
        );
    }

    @Test
    @DisplayName("POST /resend-email - Should normalize email to lowercase")
    void resendEmail_UppercaseEmail_NormalizesToLowercase() throws Exception {
        // Arrange
        User user = createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);
        user.setLastVerificationEmailSent(LocalDateTime.now().minusMinutes(10));
        userRepository.save(user);

        ResendEmailRequestDTO requestDTO = new ResendEmailRequestDTO();
        requestDTO.setEmail("TEST@GEARUP.COM");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/resend-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(emailService, times(1)).sendVerificationEmail(
                eq(TEST_EMAIL),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("POST /resend-email - Should return 400 when user not found")
    void resendEmail_UserNotFound_ReturnsBadRequest() throws Exception {
        // Arrange
        ResendEmailRequestDTO requestDTO = new ResendEmailRequestDTO();
        requestDTO.setEmail("nonexistent@gearup.com");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/resend-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /resend-email - Should return 400 when user already verified")
    void resendEmail_UserAlreadyVerified_ReturnsBadRequest() throws Exception {
        // Arrange
        createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);

        ResendEmailRequestDTO requestDTO = new ResendEmailRequestDTO();
        requestDTO.setEmail(TEST_EMAIL);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/resend-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("already verified")));
    }

    @Test
    @DisplayName("POST /resend-email - Should return 400 during cooldown period")
    void resendEmail_DuringCooldown_ReturnsBadRequest() throws Exception {
        // Arrange
        User user = createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);
        user.setLastVerificationEmailSent(LocalDateTime.now().minusSeconds(30)); // Recent timestamp
        userRepository.save(user);

        ResendEmailRequestDTO requestDTO = new ResendEmailRequestDTO();
        requestDTO.setEmail(TEST_EMAIL);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/resend-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("wait")));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /login - Should login successfully with valid credentials")
    void login_ValidCredentials_Success() throws Exception {
        // Arrange
        createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail(TEST_EMAIL);
        loginDTO.setPassword(TEST_PASSWORD);

        // Act & Assert
        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // Verify refresh token cookie is set
        String cookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(cookieHeader).contains("refreshToken");
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("Secure");
    }

    @Test
    @DisplayName("POST /login - Should return 401 for invalid credentials")
    void login_InvalidPassword_ReturnsUnauthorized() throws Exception {
        // Arrange
        createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail(TEST_EMAIL);
        loginDTO.setPassword("WrongPassword@123");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid credentials")));
    }

    @Test
    @DisplayName("POST /login - Should return 401 for non-existent user")
    void login_UserNotFound_ReturnsUnauthorized() throws Exception {
        // Arrange
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail("nonexistent@gearup.com");
        loginDTO.setPassword(TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login - Should return 400 for unverified user")
    void login_UnverifiedUser_ReturnsUnauthorized() throws Exception {
        // Arrange
        createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail(TEST_EMAIL);
        loginDTO.setPassword(TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login - Should update last login timestamp")
    void login_ValidCredentials_UpdatesLastLogin() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        LocalDateTime beforeLogin = LocalDateTime.now();

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail(TEST_EMAIL);
        loginDTO.setPassword(TEST_PASSWORD);

        // Act
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk());

        // Assert
        User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getLastLogin()).isNotNull();
        assertThat(updatedUser.getLastLogin()).isAfterOrEqualTo(beforeLogin);
    }

    @Test
    @DisplayName("POST /login - Should return 400 for missing credentials")
    void login_MissingCredentials_ReturnsBadRequest() throws Exception {
        // Arrange - Empty login DTO
        UserLoginDTO loginDTO = new UserLoginDTO();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    @DisplayName("POST /refresh - Should refresh access token with valid refresh token")
    void refreshToken_ValidRefreshToken_Success() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String refreshToken = jwtService.generateRefreshToken(userPrinciple);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("POST /refresh - Should return 401 for missing refresh token")
    void refreshToken_MissingToken_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /refresh - Should return 500 for invalid refresh token")
    void refreshToken_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Arrange
        Cookie refreshCookie = new Cookie("refreshToken", "invalid.token.here");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /refresh - Should return 500 for expired refresh token")
    void refreshToken_ExpiredToken_ReturnsUnauthorized() throws Exception {
        // Arrange - Create expired token (this would require a helper method)
        Cookie refreshCookie = new Cookie("refreshToken", "expired.refresh.token");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isInternalServerError());
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    @DisplayName("POST /logout - Should logout successfully with valid refresh token")
    void logout_ValidRefreshToken_Success() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String refreshToken = jwtService.generateRefreshToken(userPrinciple);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

        // Act & Assert
        MvcResult result = mockMvc.perform(post(BASE_URL + "/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // Verify cookie is cleared (maxAge = 0)
        String cookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(cookieHeader).contains("refreshToken");
        assertThat(cookieHeader).contains("Max-Age=0");
    }

    @Test
    @DisplayName("POST /logout - Should return 400 when no refresh token present")
    void logout_NoRefreshToken_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("No active session")));
    }

    @Test
    @DisplayName("POST /logout - Should return 400 for empty refresh token")
    void logout_EmptyRefreshToken_ReturnsBadRequest() throws Exception {
        // Arrange
        Cookie emptyRefreshCookie = new Cookie("refreshToken", "");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/logout")
                        .cookie(emptyRefreshCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    @DisplayName("POST /change-password - Should change password successfully for authenticated user")
    void changePassword_ValidRequest_Success() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String accessToken = jwtService.generateAccessToken(userPrinciple);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(TEST_PASSWORD);
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.data.message").exists())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false));

        // Verify password was changed in database
        User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches("NewPassword@123", updatedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST /change-password - Should return 400 when not authenticated")
    void changePassword_NoAuthentication_ReturnsUnauthorized() throws Exception {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(TEST_PASSWORD);
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /change-password - Should return 400 for wrong current password")
    void changePassword_WrongCurrentPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String accessToken = jwtService.generateAccessToken(userPrinciple);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("WrongPassword@123");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /change-password - Should return 400 for mismatched new passwords")
    void changePassword_MismatchedPasswords_ReturnsBadRequest() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String accessToken = jwtService.generateAccessToken(userPrinciple);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(TEST_PASSWORD);
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("DifferentPassword@123");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /change-password - Should return 400 for weak new password")
    void changePassword_WeakNewPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String accessToken = jwtService.generateAccessToken(userPrinciple);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(TEST_PASSWORD);
        request.setNewPassword("weak");
        request.setConfirmPassword("weak");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Password")));
    }

    @Test
    @DisplayName("POST /change-password - Should work for employee with requiresPasswordChange flag")
    void changePassword_EmployeeWithRequiredChange_Success() throws Exception {
        // Arrange
        User employee = createVerifiedUser("employee@gearup.com", TEST_PASSWORD, "Employee User", UserRole.EMPLOYEE);
        employee.setRequiresPasswordChange(true);
        userRepository.save(employee);

        UserPrinciple userPrinciple = new UserPrinciple(employee);
        String accessToken = jwtService.generateAccessToken(userPrinciple);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(TEST_PASSWORD);
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false));

        // Verify flag was reset
        User updatedEmployee = userRepository.findByEmail("employee@gearup.com").orElse(null);
        assertThat(updatedEmployee).isNotNull();
        assertThat(updatedEmployee.getRequiresPasswordChange()).isFalse();
    }

    // ==================== PASSWORD STATUS TESTS ====================

    @Test
    @DisplayName("GET /password-status - Should return password status for authenticated user")
    void getPasswordStatus_AuthenticatedUser_ReturnsStatus() throws Exception {
        // Arrange
        User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String accessToken = jwtService.generateAccessToken(userPrinciple);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/password-status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Password status retrieved"))
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false))
                .andExpect(jsonPath("$.data.message").value(containsString("up to date")));
    }

    @Test
    @DisplayName("GET /password-status - Should return true for employee requiring password change")
    void getPasswordStatus_EmployeeRequiresChange_ReturnsTrue() throws Exception {
        // Arrange
        User employee = createVerifiedUser("employee@gearup.com", TEST_PASSWORD, "Employee User", UserRole.EMPLOYEE);
        employee.setRequiresPasswordChange(true);
        userRepository.save(employee);

        UserPrinciple userPrinciple = new UserPrinciple(employee);
        String accessToken = jwtService.generateAccessToken(userPrinciple);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/password-status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(true))
                .andExpect(jsonPath("$.data.message").value(containsString("required")));
    }

    @Test
    @DisplayName("GET /password-status - Should return 400 when not authenticated")
    void getPasswordStatus_NoAuthentication_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/password-status"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /password-status - Should return 400 for expired token")
    void getPasswordStatus_ExpiredToken_ReturnsUnauthorized() throws Exception {
        // Arrange - Use expired token
        String expiredToken = "expired.jwt.token";

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/password-status")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isBadRequest());
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    @DisplayName("Integration - Full registration to login flow")
    void integrationScenario_RegisterVerifyLogin_Success() throws Exception {
        // Step 1: Register
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(TEST_EMAIL);
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName(TEST_NAME);

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated());

        // Step 2: Verify Email
        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        UserPrinciple userPrinciple = new UserPrinciple(user);
        String verificationToken = jwtService.generateEmailVerificationToken(userPrinciple);

        mockMvc.perform(get(BASE_URL + "/verify-email")
                        .param("token", verificationToken))
                .andExpect(status().isOk());

        // Step 3: Login
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail(TEST_EMAIL);
        loginDTO.setPassword(TEST_PASSWORD);

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("Integration - Login, change password, login with new password")
    void integrationScenario_LoginChangePasswordLoginAgain_Success() throws Exception {
        // Step 1: Create and login
        createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail(TEST_EMAIL);
        loginDTO.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody)
                .path("data").path("accessToken").asText();

        // Step 2: Change password
        PasswordChangeRequest changeRequest = new PasswordChangeRequest();
        changeRequest.setCurrentPassword(TEST_PASSWORD);
        changeRequest.setNewPassword("NewPassword@456");
        changeRequest.setConfirmPassword("NewPassword@456");

        mockMvc.perform(post(BASE_URL + "/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk());

        // Step 3: Login with new password
        UserLoginDTO newLoginDTO = new UserLoginDTO();
        newLoginDTO.setEmail(TEST_EMAIL);
        newLoginDTO.setPassword("NewPassword@456");

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("Integration - Refresh token flow")
    void integrationScenario_LoginRefreshLogout_Success() throws Exception {
        // Step 1: Login
        createVerifiedUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, UserRole.CUSTOMER);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setEmail(TEST_EMAIL);
        loginDTO.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract refresh token from cookie
        String cookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        assertThat(cookieHeader).isNotNull();
        String refreshToken = extractRefreshTokenFromCookie(cookieHeader);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

        // Step 2: Refresh access token
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());

        // Step 3: Logout
        mockMvc.perform(post(BASE_URL + "/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    // ==================== HELPER METHODS ====================

    private User createVerifiedUser(String email, String password, String name, UserRole role) {
        User user = User.builder()
                .email(email.toLowerCase().trim())
                .name(name)
                .password(passwordEncoder.encode(password))
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

    private User createUnverifiedUser(String email, String password, String name) {
        User user = User.builder()
                .email(email.toLowerCase().trim())
                .name(name)
                .password(passwordEncoder.encode(password))
                .role(UserRole.CUSTOMER)
                .isVerified(false)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private String extractRefreshTokenFromCookie(String cookieHeader) {
        // Extract token value from Set-Cookie header
        // Format: refreshToken=<token>; HttpOnly; Secure; ...
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            if (part.trim().startsWith("refreshToken=")) {
                return part.split("=")[1].trim();
            }
        }
        return null;
    }
}

