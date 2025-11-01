package com.ead.gearup.unit.service;

import com.ead.gearup.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive unit tests for EmailService
 * Tests email sending functionality with JavaMailSender and Thymeleaf templates
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String TEST_EMAIL = "test@gearup.com";
    private static final String TEST_NAME = "Test User";
    private static final String TEST_VERIFICATION_URL = "http://localhost:8080/verify?token=abc123";
    private static final String TEST_PASSWORD = "TempPassword123!";
    private static final String TEST_ROLE = "Mechanic";
    private static final String TEST_SPECIALIZATION = "Engine Specialist";

    @BeforeEach
    void setUp() {
        // Default: email enabled for most tests
        ReflectionTestUtils.setField(emailService, "emailVerificationEnabled", true);
        // Use lenient stubbing to avoid unnecessary stubbing exceptions in disabled email tests
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ============================================
    // SEND VERIFICATION EMAIL - SUCCESS TESTS
    // ============================================

    @Nested
    @DisplayName("Send Verification Email Success Tests")
    class SendVerificationEmailSuccessTests {

        @Test
        @DisplayName("Should send verification email with correct parameters")
        void sendVerificationEmail_ValidParameters_Success() {
            // Arrange
            when(templateEngine.process(eq("verification-email.html"), any(Context.class)))
                    .thenReturn("<html>Verification Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
            verify(templateEngine).process(eq("verification-email.html"), any(Context.class));
        }

        @Test
        @DisplayName("Should pass correct context variables to template")
        void sendVerificationEmail_PassesContextVariables_Correctly() {
            // Arrange
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("verification-email.html"), contextCaptor.capture()))
                    .thenReturn("<html>Verification Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(capturedContext.getVariable("verificationUrl")).isEqualTo(TEST_VERIFICATION_URL);
        }

        @Test
        @DisplayName("Should use correct email template")
        void sendVerificationEmail_UsesCorrectTemplate() {
            // Arrange
            when(templateEngine.process(eq("verification-email.html"), any(Context.class)))
                    .thenReturn("<html>Verification Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL);

            // Assert
            verify(templateEngine).process(eq("verification-email.html"), any(Context.class));
        }

        @Test
        @DisplayName("Should handle HTML content in template")
        void sendVerificationEmail_HandlesHtmlContent() {
            // Arrange
            String htmlContent = "<html><body><h1>Verify Email</h1><a href='" + TEST_VERIFICATION_URL + "'>Click here</a></body></html>";
            when(templateEngine.process(eq("verification-email.html"), any(Context.class)))
                    .thenReturn(htmlContent);

            // Act & Assert - Should not throw
            assertThatCode(() -> emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should send email only once")
        void sendVerificationEmail_SendsEmailOnce() {
            // Arrange
            when(templateEngine.process(eq("verification-email.html"), any(Context.class)))
                    .thenReturn("<html>Verification Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL);

            // Assert
            verify(mailSender, times(1)).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void sendVerificationEmail_SpecialCharactersInName_Success() {
            // Arrange
            String nameWithSpecialChars = "José María O'Brien-González";
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("verification-email.html"), contextCaptor.capture()))
                    .thenReturn("<html>Verification Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, nameWithSpecialChars, TEST_VERIFICATION_URL);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("name")).isEqualTo(nameWithSpecialChars);
        }

        @Test
        @DisplayName("Should handle different URL formats")
        void sendVerificationEmail_DifferentUrlFormats_Success() {
            // Arrange
            String httpsUrl = "https://gearup.com/verify?token=xyz&user=123";
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("verification-email.html"), contextCaptor.capture()))
                    .thenReturn("<html>Verification Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, httpsUrl);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("verificationUrl")).isEqualTo(httpsUrl);
        }
    }

    // ============================================
    // SEND VERIFICATION EMAIL - EMAIL DISABLED
    // ============================================

    @Nested
    @DisplayName("Send Verification Email - Email Disabled Tests")
    class SendVerificationEmailDisabledTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailService, "emailVerificationEnabled", false);
        }

        @Test
        @DisplayName("Should not send email when disabled")
        void sendVerificationEmail_EmailDisabled_DoesNotSendEmail() {
            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL);

            // Assert
            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
            verify(templateEngine, never()).process(anyString(), any(Context.class));
        }

        @Test
        @DisplayName("Should not throw exception when disabled")
        void sendVerificationEmail_EmailDisabled_NoException() {
            // Act & Assert
            assertThatCode(() -> emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should return immediately when disabled")
        void sendVerificationEmail_EmailDisabled_ReturnsImmediately() {
            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL);

            // Assert - No interactions with any dependencies
            verifyNoInteractions(mailSender, templateEngine);
        }
    }

    // ============================================
    // SEND VERIFICATION EMAIL - ERROR HANDLING
    // ============================================

    @Nested
    @DisplayName("Send Verification Email Error Handling Tests")
    class SendVerificationEmailErrorTests {

        @Test
        @DisplayName("Should handle template processing exception")
        void sendVerificationEmail_TemplateException_ThrowsException() {
            // Arrange
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenThrow(new RuntimeException("Template not found"));

            // Act & Assert
            assertThatThrownBy(() -> emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ============================================
    // SEND EMPLOYEE CREDENTIALS - SUCCESS TESTS
    // ============================================

    @Nested
    @DisplayName("Send Employee Credentials Success Tests")
    class SendEmployeeCredentialsSuccessTests {

        @Test
        @DisplayName("Should send employee credentials email with all parameters")
        void sendEmployeeCredentials_ValidParameters_Success() {
            // Arrange
            when(templateEngine.process(eq("employee-credentials.html"), any(Context.class)))
                    .thenReturn("<html>Employee Credentials</html>");

            // Act
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, TEST_ROLE, TEST_SPECIALIZATION);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
            verify(templateEngine).process(eq("employee-credentials.html"), any(Context.class));
        }

        @Test
        @DisplayName("Should pass all context variables to template")
        void sendEmployeeCredentials_PassesAllContextVariables() {
            // Arrange
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("employee-credentials.html"), contextCaptor.capture()))
                    .thenReturn("<html>Employee Credentials</html>");

            // Act
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, TEST_ROLE, TEST_SPECIALIZATION);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(capturedContext.getVariable("email")).isEqualTo(TEST_EMAIL);
            assertThat(capturedContext.getVariable("temporaryPassword")).isEqualTo(TEST_PASSWORD);
            assertThat(capturedContext.getVariable("role")).isEqualTo(TEST_ROLE);
            assertThat(capturedContext.getVariable("specialization")).isEqualTo(TEST_SPECIALIZATION);
            assertThat(capturedContext.getVariable("loginUrl")).isEqualTo("http://localhost:3000/login");
        }

        @Test
        @DisplayName("Should use correct employee credentials template")
        void sendEmployeeCredentials_UsesCorrectTemplate() {
            // Arrange
            when(templateEngine.process(eq("employee-credentials.html"), any(Context.class)))
                    .thenReturn("<html>Employee Credentials</html>");

            // Act
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, TEST_ROLE, TEST_SPECIALIZATION);

            // Assert
            verify(templateEngine).process(eq("employee-credentials.html"), any(Context.class));
        }

        @Test
        @DisplayName("Should handle null role")
        void sendEmployeeCredentials_NullRole_Success() {
            // Arrange
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("employee-credentials.html"), contextCaptor.capture()))
                    .thenReturn("<html>Employee Credentials</html>");

            // Act
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, null, TEST_SPECIALIZATION);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("role")).isNull();
        }

        @Test
        @DisplayName("Should handle different specializations")
        void sendEmployeeCredentials_DifferentSpecializations_Success() {
            // Arrange
            String specialization = "Electrical Systems & Diagnostics";
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("employee-credentials.html"), contextCaptor.capture()))
                    .thenReturn("<html>Employee Credentials</html>");

            // Act
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, TEST_ROLE, specialization);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("specialization")).isEqualTo(specialization);
        }
    }

    // ============================================
    // SEND EMPLOYEE CREDENTIALS - EMAIL DISABLED
    // ============================================

    @Nested
    @DisplayName("Send Employee Credentials - Email Disabled Tests")
    class SendEmployeeCredentialsDisabledTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailService, "emailVerificationEnabled", false);
        }

        @Test
        @DisplayName("Should not send email when disabled")
        void sendEmployeeCredentials_EmailDisabled_DoesNotSendEmail() {
            // Act
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, TEST_ROLE, TEST_SPECIALIZATION);

            // Assert
            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
            verify(templateEngine, never()).process(anyString(), any(Context.class));
        }

        @Test
        @DisplayName("Should not throw exception when disabled")
        void sendEmployeeCredentials_EmailDisabled_NoException() {
            // Act & Assert
            assertThatCode(() -> emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, TEST_ROLE, TEST_SPECIALIZATION))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================
    // SEND EMPLOYEE PASSWORD RESET - SUCCESS TESTS
    // ============================================

    @Nested
    @DisplayName("Send Employee Password Reset Success Tests")
    class SendEmployeePasswordResetSuccessTests {

        @Test
        @DisplayName("Should send password reset email with correct parameters")
        void sendEmployeePasswordReset_ValidParameters_Success() {
            // Arrange
            when(templateEngine.process(eq("employee-password-reset.html"), any(Context.class)))
                    .thenReturn("<html>Password Reset</html>");

            // Act
            emailService.sendEmployeePasswordReset(TEST_EMAIL, TEST_NAME, TEST_PASSWORD);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
            verify(templateEngine).process(eq("employee-password-reset.html"), any(Context.class));
        }

        @Test
        @DisplayName("Should pass correct context variables to template")
        void sendEmployeePasswordReset_PassesContextVariables() {
            // Arrange
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("employee-password-reset.html"), contextCaptor.capture()))
                    .thenReturn("<html>Password Reset</html>");

            // Act
            emailService.sendEmployeePasswordReset(TEST_EMAIL, TEST_NAME, TEST_PASSWORD);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(capturedContext.getVariable("email")).isEqualTo(TEST_EMAIL);
            assertThat(capturedContext.getVariable("temporaryPassword")).isEqualTo(TEST_PASSWORD);
            assertThat(capturedContext.getVariable("loginUrl")).isEqualTo("http://localhost:3000/login");
        }

        @Test
        @DisplayName("Should use correct password reset template")
        void sendEmployeePasswordReset_UsesCorrectTemplate() {
            // Arrange
            when(templateEngine.process(eq("employee-password-reset.html"), any(Context.class)))
                    .thenReturn("<html>Password Reset</html>");

            // Act
            emailService.sendEmployeePasswordReset(TEST_EMAIL, TEST_NAME, TEST_PASSWORD);

            // Assert
            verify(templateEngine).process(eq("employee-password-reset.html"), any(Context.class));
        }

        @Test
        @DisplayName("Should handle complex password with special characters")
        void sendEmployeePasswordReset_ComplexPassword_Success() {
            // Arrange
            String complexPassword = "P@ssw0rd!#$%^&*()";
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("employee-password-reset.html"), contextCaptor.capture()))
                    .thenReturn("<html>Password Reset</html>");

            // Act
            emailService.sendEmployeePasswordReset(TEST_EMAIL, TEST_NAME, complexPassword);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("temporaryPassword")).isEqualTo(complexPassword);
        }
    }

    // ============================================
    // SEND EMPLOYEE PASSWORD RESET - EMAIL DISABLED
    // ============================================

    @Nested
    @DisplayName("Send Employee Password Reset - Email Disabled Tests")
    class SendEmployeePasswordResetDisabledTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailService, "emailVerificationEnabled", false);
        }

        @Test
        @DisplayName("Should not send email when disabled")
        void sendEmployeePasswordReset_EmailDisabled_DoesNotSendEmail() {
            // Act
            emailService.sendEmployeePasswordReset(TEST_EMAIL, TEST_NAME, TEST_PASSWORD);

            // Assert
            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
            verify(templateEngine, never()).process(anyString(), any(Context.class));
        }

        @Test
        @DisplayName("Should not throw exception when disabled")
        void sendEmployeePasswordReset_EmailDisabled_NoException() {
            // Act & Assert
            assertThatCode(() -> emailService.sendEmployeePasswordReset(TEST_EMAIL, TEST_NAME, TEST_PASSWORD))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================
    // EDGE CASES AND INTEGRATION TESTS
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long email addresses")
        void sendVerificationEmail_LongEmailAddress_Success() {
            // Arrange
            String longEmail = "very.long.email.address.with.multiple.dots.and.subdomains@long.subdomain.company.co.uk";
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("verification-email.html"), contextCaptor.capture()))
                    .thenReturn("<html>Email</html>");

            // Act
            emailService.sendVerificationEmail(longEmail, TEST_NAME, TEST_VERIFICATION_URL);

            // Assert
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle very long names")
        void sendVerificationEmail_LongName_Success() {
            // Arrange
            String longName = "Alexander Christopher Montgomery-Wellington-Smythe III";
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("verification-email.html"), contextCaptor.capture()))
                    .thenReturn("<html>Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, longName, TEST_VERIFICATION_URL);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("name")).isEqualTo(longName);
        }

        @Test
        @DisplayName("Should handle empty string role and specialization")
        void sendEmployeeCredentials_EmptyStrings_Success() {
            // Arrange
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("employee-credentials.html"), contextCaptor.capture()))
                    .thenReturn("<html>Email</html>");

            // Act
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, "", "");

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("role")).isEqualTo("");
            assertThat(capturedContext.getVariable("specialization")).isEqualTo("");
        }

        @Test
        @DisplayName("Should handle URLs with query parameters")
        void sendVerificationEmail_UrlWithQueryParams_Success() {
            // Arrange
            String urlWithParams = "https://gearup.com/verify?token=abc123&userId=456&redirect=/dashboard";
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("verification-email.html"), contextCaptor.capture()))
                    .thenReturn("<html>Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, urlWithParams);

            // Assert
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("verificationUrl")).isEqualTo(urlWithParams);
        }

        @Test
        @DisplayName("Should handle template returning very large HTML content")
        void sendVerificationEmail_LargeHtmlContent_Success() {
            // Arrange
            StringBuilder largeHtml = new StringBuilder("<html><body>");
            for (int i = 0; i < 1000; i++) {
                largeHtml.append("<p>Line ").append(i).append("</p>");
            }
            largeHtml.append("</body></html>");
            when(templateEngine.process(eq("verification-email.html"), any(Context.class)))
                    .thenReturn(largeHtml.toString());

            // Act & Assert
            assertThatCode(() -> emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle multiple emails sent in sequence")
        void sendMultipleEmails_Sequential_Success() {
            // Arrange
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenReturn("<html>Email</html>");

            // Act
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL);
            emailService.sendEmployeeCredentials(TEST_EMAIL, TEST_NAME, TEST_PASSWORD, TEST_ROLE, TEST_SPECIALIZATION);
            emailService.sendEmployeePasswordReset(TEST_EMAIL, TEST_NAME, TEST_PASSWORD);

            // Assert
            verify(mailSender, times(3)).send(mimeMessage);
            verify(templateEngine).process(eq("verification-email.html"), any(Context.class));
            verify(templateEngine).process(eq("employee-credentials.html"), any(Context.class));
            verify(templateEngine).process(eq("employee-password-reset.html"), any(Context.class));
        }

        @Test
        @DisplayName("Should handle template with unicode characters")
        void sendVerificationEmail_UnicodeInTemplate_Success() {
            // Arrange
            String unicodeHtml = "<html><body><p>Hello 你好 مرحبا שלום</p></body></html>";
            when(templateEngine.process(eq("verification-email.html"), any(Context.class)))
                    .thenReturn(unicodeHtml);

            // Act & Assert
            assertThatCode(() -> emailService.sendVerificationEmail(TEST_EMAIL, TEST_NAME, TEST_VERIFICATION_URL))
                    .doesNotThrowAnyException();
        }
    }
}

