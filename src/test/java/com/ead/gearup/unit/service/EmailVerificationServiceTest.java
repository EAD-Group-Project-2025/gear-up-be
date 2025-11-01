package com.ead.gearup.unit.service;

import com.ead.gearup.fixtures.UserFixtures;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.EmailService;
import com.ead.gearup.service.EmailVerificationService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive unit tests for EmailVerificationService
 * Tests email verification token generation and sending functionality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService Unit Tests")
class EmailVerificationServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private static final String TEST_APP_BASE_URL = "http://localhost:8080";
    private static final String TEST_FRONTEND_URL = "http://localhost:3000";
    private static final String TEST_VERIFICATION_TOKEN = "test.verification.token";

    private User testUser;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(emailVerificationService, "appBaseUrl", TEST_APP_BASE_URL);
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", TEST_FRONTEND_URL);
        
        // Create test user
        testUser = UserFixtures.unverifiedCustomer();
    }

    // ============================================
    // EMAIL VERIFICATION DISABLED TESTS
    // ============================================

    @Nested
    @DisplayName("Email Verification Disabled Tests")
    class EmailVerificationDisabledTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailVerificationService, "emailVerificationEnabled", false);
        }

        @Test
        @DisplayName("Should auto-verify user when email verification is disabled")
        void sendVerificationEmail_VerificationDisabled_AutoVerifiesUser() {
            // Arrange
            assertThat(testUser.getIsVerified()).isFalse();

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            assertThat(testUser.getIsVerified()).isTrue();
        }

        @Test
        @DisplayName("Should save user after auto-verification")
        void sendVerificationEmail_VerificationDisabled_SavesUser() {
            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(userRepository, times(1)).save(testUser);
        }

        @Test
        @DisplayName("Should not call email service when verification is disabled")
        void sendVerificationEmail_VerificationDisabled_DoesNotSendEmail() {
            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should not generate JWT token when verification is disabled")
        void sendVerificationEmail_VerificationDisabled_DoesNotGenerateToken() {
            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(jwtService, never()).generateEmailVerificationToken(any(UserDetails.class));
        }

        @Test
        @DisplayName("Should handle already verified user gracefully")
        void sendVerificationEmail_VerificationDisabled_AlreadyVerifiedUser() {
            // Arrange
            testUser.setIsVerified(true);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            assertThat(testUser.getIsVerified()).isTrue();
            verify(userRepository, times(1)).save(testUser);
        }
    }

    // ============================================
    // EMAIL VERIFICATION ENABLED TESTS
    // ============================================

    @Nested
    @DisplayName("Email Verification Enabled Tests")
    class EmailVerificationEnabledTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailVerificationService, "emailVerificationEnabled", true);
        }

        @Test
        @DisplayName("Should generate email verification token")
        void sendVerificationEmail_VerificationEnabled_GeneratesToken() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            ArgumentCaptor<UserDetails> userDetailsCaptor = ArgumentCaptor.forClass(UserDetails.class);
            verify(jwtService, times(1)).generateEmailVerificationToken(userDetailsCaptor.capture());
            
            UserDetails capturedUserDetails = userDetailsCaptor.getValue();
            assertThat(capturedUserDetails).isInstanceOf(UserPrinciple.class);
            assertThat(capturedUserDetails.getUsername()).isEqualTo(testUser.getEmail());
        }

        @Test
        @DisplayName("Should send verification email with correct parameters")
        void sendVerificationEmail_VerificationEnabled_SendsEmail() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            String expectedUrl = TEST_APP_BASE_URL + "/api/v1/auth/verify-email?token=" + TEST_VERIFICATION_TOKEN;
            
            verify(emailService, times(1)).sendVerificationEmail(
                    eq(testUser.getEmail()),
                    eq(testUser.getName()),
                    eq(expectedUrl)
            );
        }

        @Test
        @DisplayName("Should construct verification URL correctly")
        void sendVerificationEmail_VerificationEnabled_ConstructsCorrectUrl() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    anyString(),
                    anyString(),
                    urlCaptor.capture()
            );

            String capturedUrl = urlCaptor.getValue();
            assertThat(capturedUrl).startsWith(TEST_APP_BASE_URL);
            assertThat(capturedUrl).contains("/api/v1/auth/verify-email");
            assertThat(capturedUrl).contains("?token=");
            assertThat(capturedUrl).endsWith(TEST_VERIFICATION_TOKEN);
        }

        @Test
        @DisplayName("Should not auto-verify user when verification is enabled")
        void sendVerificationEmail_VerificationEnabled_DoesNotAutoVerify() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            assertThat(testUser.getIsVerified()).isFalse();

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            assertThat(testUser.getIsVerified()).isFalse();
        }

        @Test
        @DisplayName("Should not save user when verification is enabled")
        void sendVerificationEmail_VerificationEnabled_DoesNotSaveUser() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle user with long name")
        void sendVerificationEmail_VerificationEnabled_LongUserName() {
            // Arrange
            testUser.setName("A Very Long User Name That Might Cause Issues In Some Systems");
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    eq(testUser.getEmail()),
                    eq(testUser.getName()),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle user with special characters in name")
        void sendVerificationEmail_VerificationEnabled_SpecialCharactersInName() {
            // Arrange
            testUser.setName("José María O'Brien-González");
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    eq(testUser.getEmail()),
                    eq(testUser.getName()),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle different base URLs correctly")
        void sendVerificationEmail_DifferentBaseUrl_ConstructsCorrectUrl() {
            // Arrange
            String customBaseUrl = "https://production.gearup.com";
            ReflectionTestUtils.setField(emailVerificationService, "appBaseUrl", customBaseUrl);
            
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    anyString(),
                    anyString(),
                    urlCaptor.capture()
            );

            String capturedUrl = urlCaptor.getValue();
            assertThat(capturedUrl).startsWith(customBaseUrl);
            assertThat(capturedUrl).contains(TEST_VERIFICATION_TOKEN);
        }
    }

    // ============================================
    // EXCEPTION HANDLING TESTS
    // ============================================

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailVerificationService, "emailVerificationEnabled", true);
        }

        @Test
        @DisplayName("Should throw RuntimeException when token generation fails")
        void sendVerificationEmail_TokenGenerationFails_ThrowsRuntimeException() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenThrow(new RuntimeException("Token generation failed"));

            // Act & Assert
            assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send verification email")
                    .hasMessageContaining("Token generation failed");
        }

        @Test
        @DisplayName("Should throw RuntimeException when email service fails")
        void sendVerificationEmail_EmailServiceFails_ThrowsRuntimeException() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);
            
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService)
                    .sendVerificationEmail(anyString(), anyString(), anyString());

            // Act & Assert
            assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send verification email")
                    .hasMessageContaining("Email service unavailable");
        }

        @Test
        @DisplayName("Should include original exception message in wrapped exception")
        void sendVerificationEmail_Exception_IncludesOriginalMessage() {
            // Arrange
            String originalMessage = "SMTP server connection timeout";
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);
            
            doThrow(new RuntimeException(originalMessage))
                    .when(emailService)
                    .sendVerificationEmail(anyString(), anyString(), anyString());

            // Act & Assert
            assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(originalMessage);
        }

        @Test
        @DisplayName("Should propagate exception cause")
        void sendVerificationEmail_Exception_PropagatesCause() {
            // Arrange
            RuntimeException cause = new RuntimeException("Original cause");
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);
            
            doThrow(cause)
                    .when(emailService)
                    .sendVerificationEmail(anyString(), anyString(), anyString());

            // Act & Assert
            assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(cause);
        }

        @Test
        @DisplayName("Should not throw exception when verification is disabled even if services fail")
        void sendVerificationEmail_VerificationDisabled_NoExceptionEvenIfServicesWouldFail() {
            // Arrange
            ReflectionTestUtils.setField(emailVerificationService, "emailVerificationEnabled", false);
            
            // Setup lenient mocks to throw exceptions (but they shouldn't be called)
            lenient().when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenThrow(new RuntimeException("Should not be called"));
            lenient().doThrow(new RuntimeException("Should not be called"))
                    .when(emailService)
                    .sendVerificationEmail(anyString(), anyString(), anyString());

            // Act & Assert - Should not throw
            assertThatCode(() -> emailVerificationService.sendVerificationEmail(testUser))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================
    // EDGE CASES AND BOUNDARY TESTS
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailVerificationService, "emailVerificationEnabled", true);
        }

        @Test
        @DisplayName("Should handle user with minimal name")
        void sendVerificationEmail_MinimalName_Success() {
            // Arrange
            testUser.setName("A");
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    eq(testUser.getEmail()),
                    eq("A"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle empty app base URL path")
        void sendVerificationEmail_EmptyBasePath_ConstructsValidUrl() {
            // Arrange
            ReflectionTestUtils.setField(emailVerificationService, "appBaseUrl", "http://localhost:8080");
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    anyString(),
                    anyString(),
                    urlCaptor.capture()
            );

            assertThat(urlCaptor.getValue()).matches("^https?://[^/]+/api/v1/auth/verify-email\\?token=.+$");
        }

        @Test
        @DisplayName("Should handle very long verification token")
        void sendVerificationEmail_LongToken_Success() {
            // Arrange
            String longToken = "a".repeat(500); // Very long token
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(longToken);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendVerificationEmail(
                    anyString(),
                    anyString(),
                    urlCaptor.capture()
            );

            assertThat(urlCaptor.getValue()).endsWith(longToken);
        }

        @Test
        @DisplayName("Should create UserPrinciple from unverified user")
        void sendVerificationEmail_UnverifiedUser_CreatesUserPrinciple() {
            // Arrange
            testUser.setIsVerified(false);
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            ArgumentCaptor<UserDetails> userDetailsCaptor = ArgumentCaptor.forClass(UserDetails.class);
            verify(jwtService).generateEmailVerificationToken(userDetailsCaptor.capture());
            
            assertThat(userDetailsCaptor.getValue()).isInstanceOf(UserPrinciple.class);
        }

        @Test
        @DisplayName("Should handle base URL with trailing slash")
        void sendVerificationEmail_BaseUrlWithTrailingSlash_ConstructsUrl() {
            // Arrange
            ReflectionTestUtils.setField(emailVerificationService, "appBaseUrl", "http://localhost:8080/");
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    anyString(),
                    anyString(),
                    urlCaptor.capture()
            );

            String url = urlCaptor.getValue();
            assertThat(url).contains("/api/v1/auth/verify-email?token=");
            assertThat(url).contains(TEST_VERIFICATION_TOKEN);
        }

        @Test
        @DisplayName("Should handle base URL without protocol")
        void sendVerificationEmail_BaseUrlWithoutProtocol_StillWorks() {
            // Arrange
            ReflectionTestUtils.setField(emailVerificationService, "appBaseUrl", "localhost:8080");
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendVerificationEmail(
                    anyString(),
                    anyString(),
                    urlCaptor.capture()
            );

            assertThat(urlCaptor.getValue()).contains("localhost:8080");
        }
    }

    // ============================================
    // INTERACTION VERIFICATION TESTS
    // ============================================

    @Nested
    @DisplayName("Service Interaction Tests")
    class ServiceInteractionTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailVerificationService, "emailVerificationEnabled", true);
        }

        @Test
        @DisplayName("Should call services in correct order")
        void sendVerificationEmail_ServiceCallOrder_IsCorrect() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            var inOrder = inOrder(jwtService, emailService);
            inOrder.verify(jwtService).generateEmailVerificationToken(any(UserDetails.class));
            inOrder.verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should not call email service if token generation fails")
        void sendVerificationEmail_TokenGenerationFails_DoesNotCallEmailService() {
            // Arrange
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenThrow(new RuntimeException("Token generation failed"));

            // Act
            try {
                emailVerificationService.sendVerificationEmail(testUser);
            } catch (RuntimeException e) {
                // Expected exception
            }

            // Assert
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should pass exact user details to services")
        void sendVerificationEmail_UserDetails_ArePassedExactly() {
            // Arrange
            String specificEmail = "specific.user@test.com";
            String specificName = "Specific User";
            testUser.setEmail(specificEmail);
            testUser.setName(specificName);
            
            when(jwtService.generateEmailVerificationToken(any(UserDetails.class)))
                    .thenReturn(TEST_VERIFICATION_TOKEN);

            // Act
            emailVerificationService.sendVerificationEmail(testUser);

            // Assert
            verify(emailService).sendVerificationEmail(
                    eq(specificEmail),
                    eq(specificName),
                    anyString()
            );
        }
    }
}

