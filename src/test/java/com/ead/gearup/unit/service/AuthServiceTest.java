package com.ead.gearup.unit.service;

import com.ead.gearup.dto.request.ResendEmailRequestDTO;
import com.ead.gearup.dto.response.JwtTokensDTO;
import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.EmailAlreadyExistsException;
import com.ead.gearup.exception.InvalidRefreshTokenException;
import com.ead.gearup.exception.ResendEmailCooldownException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.fixtures.DTOFixtures;
import com.ead.gearup.fixtures.UserFixtures;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.AuthService;
import com.ead.gearup.service.EmailVerificationService;
import com.ead.gearup.service.auth.CustomUserDetailsService;
import com.ead.gearup.service.auth.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AuthService
 * Tests all authentication and authorization related functionality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserCreateDTO userCreateDTO;

    @BeforeEach
    void setUp() {
        testUser = UserFixtures.verifiedCustomer();
        userCreateDTO = DTOFixtures.validUserCreateDTO();
    }

    // ============================================
    // CREATE USER TESTS
    // ============================================

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully with valid data")
        void createUser_ValidData_ReturnsUserResponse() {
            // Arrange
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                return user;
            });
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

            // Act
            UserResponseDTO response = authService.createUser(userCreateDTO);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("test@gearup.com");
            assertThat(response.getName()).isEqualTo("Test User");

            // Verify interactions
            verify(passwordEncoder).encode("Test@123");
            verify(userRepository).save(any(User.class));
            verify(customerRepository).save(any(Customer.class));
            verify(emailVerificationService).sendVerificationEmail(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void createUser_UppercaseEmail_NormalizesToLowercase() {
            // Arrange
            UserCreateDTO dtoWithUppercase = DTOFixtures.userCreateDTOWithUppercaseEmail();
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                return user;
            });
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

            // Act
            UserResponseDTO response = authService.createUser(dtoWithUppercase);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            assertThat(savedUser.getEmail()).isEqualTo("test@gearup.com");
            assertThat(response.getEmail()).isEqualTo("test@gearup.com");
        }

        @Test
        @DisplayName("Should trim whitespace from email")
        void createUser_EmailWithWhitespace_TrimsWhitespace() {
            // Arrange
            UserCreateDTO dtoWithWhitespace = DTOFixtures.userCreateDTOWithWhitespaceEmail();
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                return user;
            });
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

            // Act
            UserResponseDTO response = authService.createUser(dtoWithWhitespace);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            assertThat(savedUser.getEmail()).isEqualTo("test@gearup.com");
            assertThat(savedUser.getEmail()).doesNotContain(" ");
        }

        @Test
        @DisplayName("Should assign CUSTOMER role to new user")
        void createUser_ValidData_AssignsCustomerRole() {
            // Arrange
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                return user;
            });
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

            // Act
            authService.createUser(userCreateDTO);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("Should create Customer entity for CUSTOMER role")
        void createUser_CustomerRole_CreatesCustomerEntity() {
            // Arrange
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                return user;
            });
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

            // Act
            authService.createUser(userCreateDTO);

            // Assert
            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(customerCaptor.capture());
            Customer savedCustomer = customerCaptor.getValue();
            
            assertThat(savedCustomer.getUser()).isNotNull();
            assertThat(savedCustomer.getPhoneNumber()).isNull();
        }

        @Test
        @DisplayName("Should send verification email after user creation")
        void createUser_ValidData_SendsVerificationEmail() {
            // Arrange
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                return user;
            });
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

            // Act
            authService.createUser(userCreateDTO);

            // Assert
            verify(emailVerificationService, times(1)).sendVerificationEmail(any(User.class));
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when email exists")
        void createUser_DuplicateEmail_ThrowsEmailAlreadyExistsException() {
            // Arrange
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate email"));

            // Act & Assert
            assertThatThrownBy(() -> authService.createUser(userCreateDTO))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("test@gearup.com");

            verify(customerRepository, never()).save(any(Customer.class));
            verify(emailVerificationService, never()).sendVerificationEmail(any(User.class));
        }

        @Test
        @DisplayName("Should encode password before saving")
        void createUser_ValidData_EncodesPassword() {
            // Arrange
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword123");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                return user;
            });
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

            // Act
            authService.createUser(userCreateDTO);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            assertThat(savedUser.getPassword()).isEqualTo("encodedPassword123");
            assertThat(savedUser.getPassword()).isNotEqualTo("Test@123");
        }
    }

    // ============================================
    // VERIFY EMAIL TOKEN TESTS
    // ============================================

    @Nested
    @DisplayName("Verify Email Token Tests")
    class VerifyEmailTokenTests {

        @Test
        @DisplayName("Should verify email successfully with valid token")
        void verifyEmailToken_ValidToken_ReturnsTrue() {
            // Arrange
            String token = "valid.email.token";
            User unverifiedUser = UserFixtures.unverifiedCustomer();
            
            when(jwtService.extractUsername(token)).thenReturn(unverifiedUser.getEmail());
            when(jwtService.extractClaim(eq(token), any())).thenReturn("email_verification");
            when(userRepository.findByEmail(unverifiedUser.getEmail()))
                    .thenReturn(Optional.of(unverifiedUser));
            when(userRepository.save(any(User.class))).thenReturn(unverifiedUser);

            // Act
            boolean result = authService.verifyEmailToken(token);

            // Assert
            assertThat(result).isTrue();
            verify(userRepository).save(any(User.class));
            
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getIsVerified()).isTrue();
        }

        @Test
        @DisplayName("Should return true if user already verified")
        void verifyEmailToken_AlreadyVerified_ReturnsTrue() {
            // Arrange
            String token = "valid.email.token";
            User verifiedUser = UserFixtures.verifiedCustomer();
            
            when(jwtService.extractUsername(token)).thenReturn(verifiedUser.getEmail());
            when(jwtService.extractClaim(eq(token), any())).thenReturn("email_verification");
            when(userRepository.findByEmail(verifiedUser.getEmail()))
                    .thenReturn(Optional.of(verifiedUser));

            // Act
            boolean result = authService.verifyEmailToken(token);

            // Assert
            assertThat(result).isTrue();
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should return false for wrong token type")
        void verifyEmailToken_WrongTokenType_ReturnsFalse() {
            // Arrange
            String token = "wrong.type.token";
            
            when(jwtService.extractUsername(token)).thenReturn("test@gearup.com");
            when(jwtService.extractClaim(eq(token), any())).thenReturn("access");

            // Act
            boolean result = authService.verifyEmailToken(token);

            // Assert
            assertThat(result).isFalse();
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Should return false when user not found")
        void verifyEmailToken_UserNotFound_ReturnsFalse() {
            // Arrange
            String token = "valid.email.token";
            
            when(jwtService.extractUsername(token)).thenReturn("nonexistent@gearup.com");
            when(jwtService.extractClaim(eq(token), any())).thenReturn("email_verification");
            when(userRepository.findByEmail("nonexistent@gearup.com"))
                    .thenReturn(Optional.empty());

            // Act
            boolean result = authService.verifyEmailToken(token);

            // Assert
            assertThat(result).isFalse();
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should return false for expired token")
        void verifyEmailToken_ExpiredToken_ReturnsFalse() {
            // Arrange
            String expiredToken = "expired.email.token";
            
            when(jwtService.extractUsername(expiredToken))
                    .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Token expired"));

            // Act
            boolean result = authService.verifyEmailToken(expiredToken);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle malformed token gracefully")
        void verifyEmailToken_MalformedToken_ReturnsFalse() {
            // Arrange
            String malformedToken = "malformed.token";
            
            when(jwtService.extractUsername(malformedToken))
                    .thenThrow(new RuntimeException("Malformed token"));

            // Act
            boolean result = authService.verifyEmailToken(malformedToken);

            // Assert
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // RESEND EMAIL TESTS
    // ============================================

    @Nested
    @DisplayName("Resend Email Tests")
    class ResendEmailTests {

        @Test
        @DisplayName("Should resend email successfully when cooldown expired")
        void resendEmail_CooldownExpired_SendsEmail() {
            // Arrange
            ResendEmailRequestDTO requestDTO = DTOFixtures.validResendEmailRequestDTO();
            User userWithOldVerification = UserFixtures.withOldVerificationEmail();
            
            when(userRepository.findByEmail("test@gearup.com"))
                    .thenReturn(Optional.of(userWithOldVerification));
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));
            when(userRepository.save(any(User.class))).thenReturn(userWithOldVerification);

            // Act
            authService.resendEmail(requestDTO);

            // Assert
            verify(emailVerificationService).sendVerificationEmail(any(User.class));
            verify(userRepository).save(any(User.class));
            
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getLastVerificationEmailSent()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void resendEmail_UserNotFound_ThrowsUserNotFoundException() {
            // Arrange
            ResendEmailRequestDTO requestDTO = DTOFixtures.validResendEmailRequestDTO();
            
            when(userRepository.findByEmail("test@gearup.com"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.resendEmail(requestDTO))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(emailVerificationService, never()).sendVerificationEmail(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user already verified")
        void resendEmail_AlreadyVerified_ThrowsIllegalStateException() {
            // Arrange
            ResendEmailRequestDTO requestDTO = DTOFixtures.validResendEmailRequestDTO();
            User verifiedUser = UserFixtures.verifiedCustomer();
            
            when(userRepository.findByEmail("test@gearup.com"))
                    .thenReturn(Optional.of(verifiedUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.resendEmail(requestDTO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already verified");

            verify(emailVerificationService, never()).sendVerificationEmail(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception during cooldown period")
        void resendEmail_WithinCooldown_ThrowsResendEmailCooldownException() {
            // Arrange
            ResendEmailRequestDTO requestDTO = DTOFixtures.validResendEmailRequestDTO();
            User userWithRecentEmail = UserFixtures.withRecentVerificationEmail();
            
            when(userRepository.findByEmail("test@gearup.com"))
                    .thenReturn(Optional.of(userWithRecentEmail));

            // Act & Assert
            assertThatThrownBy(() -> authService.resendEmail(requestDTO))
                    .isInstanceOf(ResendEmailCooldownException.class)
                    .hasMessageContaining("Please wait");

            verify(emailVerificationService, never()).sendVerificationEmail(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase before checking")
        void resendEmail_UppercaseEmail_NormalizesToLowercase() {
            // Arrange
            ResendEmailRequestDTO requestDTO = DTOFixtures.resendEmailRequestDTOWithUppercase();
            User userWithOldVerification = UserFixtures.withOldVerificationEmail();
            
            when(userRepository.findByEmail("test@gearup.com"))
                    .thenReturn(Optional.of(userWithOldVerification));
            doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));
            when(userRepository.save(any(User.class))).thenReturn(userWithOldVerification);

            // Act
            authService.resendEmail(requestDTO);

            // Assert
            verify(userRepository).findByEmail("test@gearup.com");
        }
    }

    // ============================================
    // VERIFY USER (LOGIN) TESTS
    // ============================================

    @Nested
    @DisplayName("Verify User (Login) Tests")
    class VerifyUserTests {

        @Mock
        private Authentication authentication;

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void verifyUser_ValidCredentials_ReturnsJwtTokens() {
            // Arrange
            UserLoginDTO loginDTO = DTOFixtures.validUserLoginDTO();
            UserPrinciple userPrinciple = new UserPrinciple(testUser);
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userPrinciple);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateAccessToken(any(UserPrinciple.class), any()))
                    .thenReturn("access.token.here");
            when(jwtService.generateRefreshToken(any(UserPrinciple.class)))
                    .thenReturn("refresh.token.here");

            // Act
            JwtTokensDTO result = authService.verifyUser(loginDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access.token.here");
            assertThat(result.getRefreshToken()).isEqualTo("refresh.token.here");
            
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should update last login timestamp")
        void verifyUser_ValidCredentials_UpdatesLastLogin() {
            // Arrange
            UserLoginDTO loginDTO = DTOFixtures.validUserLoginDTO();
            UserPrinciple userPrinciple = new UserPrinciple(testUser);
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userPrinciple);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateAccessToken(any(UserPrinciple.class), any()))
                    .thenReturn("access.token.here");
            when(jwtService.generateRefreshToken(any(UserPrinciple.class)))
                    .thenReturn("refresh.token.here");

            // Act
            authService.verifyUser(loginDTO);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getLastLogin()).isNotNull();
        }

        @Test
        @DisplayName("Should include requiresPasswordChange in token claims")
        void verifyUser_EmployeeUser_IncludesPasswordChangeFlag() {
            // Arrange
            UserLoginDTO loginDTO = DTOFixtures.validUserLoginDTO();
            User employeeUser = UserFixtures.employeeUser();
            UserPrinciple userPrinciple = new UserPrinciple(employeeUser);
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userPrinciple);
            when(userRepository.save(any(User.class))).thenReturn(employeeUser);
            when(jwtService.generateAccessToken(any(UserPrinciple.class), any()))
                    .thenReturn("access.token.here");
            when(jwtService.generateRefreshToken(any(UserPrinciple.class)))
                    .thenReturn("refresh.token.here");

            // Act
            authService.verifyUser(loginDTO);

            // Assert
            ArgumentCaptor<java.util.Map> claimsCaptor = ArgumentCaptor.forClass(java.util.Map.class);
            verify(jwtService).generateAccessToken(any(UserPrinciple.class), claimsCaptor.capture());
            
            java.util.Map<String, Object> claims = claimsCaptor.getValue();
            assertThat(claims).containsKey("requiresPasswordChange");
            assertThat(claims.get("requiresPasswordChange")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for invalid credentials")
        void verifyUser_InvalidCredentials_ThrowsBadCredentialsException() {
            // Arrange
            UserLoginDTO loginDTO = DTOFixtures.userLoginDTOWithWrongPassword();
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThatThrownBy(() -> authService.verifyUser(loginDTO))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).save(any(User.class));
            verify(jwtService, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when authentication fails")
        void verifyUser_AuthenticationFailed_ThrowsException() {
            // Arrange
            UserLoginDTO loginDTO = DTOFixtures.validUserLoginDTO();
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.verifyUser(loginDTO))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("invalid credentials");
        }
    }

    // ============================================
    // REFRESH TOKEN TESTS
    // ============================================

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Mock
        private UserDetails userDetails;

        @Test
        @DisplayName("Should refresh access token with valid refresh token")
        void getRefreshAccessToken_ValidRefreshToken_ReturnsNewAccessToken() {
            // Arrange
            String refreshToken = "valid.refresh.token";
            
            when(jwtService.extractUsername(refreshToken)).thenReturn("test@gearup.com");
            when(customUserDetailsService.loadUserByUsername("test@gearup.com"))
                    .thenReturn(userDetails);
            when(jwtService.validateRefreshToken(refreshToken, userDetails)).thenReturn(true);
            when(jwtService.generateAccessToken(userDetails)).thenReturn("new.access.token");

            // Act
            LoginResponseDTO result = authService.getRefreshAccessToken(refreshToken);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new.access.token");
            
            verify(jwtService).validateRefreshToken(refreshToken, userDetails);
            verify(jwtService).generateAccessToken(userDetails);
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void getRefreshAccessToken_InvalidToken_ThrowsInvalidRefreshTokenException() {
            // Arrange
            String invalidToken = "invalid.refresh.token";
            
            when(jwtService.extractUsername(invalidToken)).thenReturn("test@gearup.com");
            when(customUserDetailsService.loadUserByUsername("test@gearup.com"))
                    .thenReturn(userDetails);
            when(jwtService.validateRefreshToken(invalidToken, userDetails)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.getRefreshAccessToken(invalidToken))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Invalid or expired refresh token");

            verify(jwtService, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("Should throw exception for expired refresh token")
        void getRefreshAccessToken_ExpiredToken_ThrowsInvalidRefreshTokenException() {
            // Arrange
            String expiredToken = "expired.refresh.token";
            
            when(jwtService.extractUsername(expiredToken)).thenReturn("test@gearup.com");
            when(customUserDetailsService.loadUserByUsername("test@gearup.com"))
                    .thenReturn(userDetails);
            when(jwtService.validateRefreshToken(expiredToken, userDetails)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.getRefreshAccessToken(expiredToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("Should handle user not found during token refresh")
        void getRefreshAccessToken_UserNotFound_ThrowsException() {
            // Arrange
            String refreshToken = "valid.refresh.token";
            
            when(jwtService.extractUsername(refreshToken)).thenReturn("nonexistent@gearup.com");
            when(customUserDetailsService.loadUserByUsername("nonexistent@gearup.com"))
                    .thenThrow(new UserNotFoundException("User not found"));

            // Act & Assert
            assertThatThrownBy(() -> authService.getRefreshAccessToken(refreshToken))
                    .isInstanceOf(UserNotFoundException.class);

            verify(jwtService, never()).validateRefreshToken(anyString(), any());
        }
    }
}

