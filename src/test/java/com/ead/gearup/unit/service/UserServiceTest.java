package com.ead.gearup.unit.service;

import com.ead.gearup.dto.user.PasswordChangeRequest;
import com.ead.gearup.dto.user.PasswordChangeResponse;
import com.ead.gearup.fixtures.DTOFixtures;
import com.ead.gearup.fixtures.UserFixtures;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserService
 * Tests password change and password requirement checking functionality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private PasswordChangeRequest passwordChangeRequest;

    private static final String TEST_EMAIL = "test@gearup.com";
    private static final String CURRENT_PASSWORD = "OldPassword123!";
    private static final String NEW_PASSWORD = "NewPassword123!";
    private static final String ENCODED_CURRENT_PASSWORD = "encodedOldPassword";
    private static final String ENCODED_NEW_PASSWORD = "encodedNewPassword";

    @BeforeEach
    void setUp() {
        testUser = UserFixtures.verifiedCustomer();
        testUser.setPassword(ENCODED_CURRENT_PASSWORD);
        
        passwordChangeRequest = DTOFixtures.validPasswordChangeRequest();
    }

    // ============================================
    // CHANGE PASSWORD - SUCCESS TESTS
    // ============================================

    @Nested
    @DisplayName("Change Password Success Tests")
    class ChangePasswordSuccessTests {

        @Test
        @DisplayName("Should change password successfully with valid data")
        void changePassword_ValidData_Success() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            PasswordChangeResponse response = userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Password changed successfully");
            assertThat(response.isRequiresPasswordChange()).isFalse();
        }

        @Test
        @DisplayName("Should encode new password")
        void changePassword_Success_EncodesNewPassword() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            verify(passwordEncoder).encode(NEW_PASSWORD);
            assertThat(testUser.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("Should save user after password change")
        void changePassword_Success_SavesUser() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("Should clear requiresPasswordChange flag")
        void changePassword_Success_ClearsRequiresPasswordChangeFlag() {
            // Arrange
            testUser.setRequiresPasswordChange(true);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            assertThat(testUser.getRequiresPasswordChange()).isFalse();
        }

        @Test
        @DisplayName("Should handle user with requiresPasswordChange already false")
        void changePassword_RequiresPasswordChangeAlreadyFalse_Success() {
            // Arrange
            testUser.setRequiresPasswordChange(false);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            PasswordChangeResponse response = userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            assertThat(response.isRequiresPasswordChange()).isFalse();
            assertThat(testUser.getRequiresPasswordChange()).isFalse();
        }

        @Test
        @DisplayName("Should verify password matching order")
        void changePassword_Success_VerifiesPasswordsInCorrectOrder() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            var inOrder = inOrder(passwordEncoder);
            inOrder.verify(passwordEncoder).matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD);
            inOrder.verify(passwordEncoder).matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD);
            inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
        }
    }

    // ============================================
    // CHANGE PASSWORD - VALIDATION FAILURE TESTS
    // ============================================

    @Nested
    @DisplayName("Change Password Validation Failure Tests")
    class ChangePasswordValidationTests {

        @Test
        @DisplayName("Should throw exception when passwords don't match")
        void changePassword_PasswordsDontMatch_ThrowsException() {
            // Arrange
            passwordChangeRequest.setConfirmPassword("DifferentPassword123!");

            // Act & Assert
            assertThatThrownBy(() -> userService.changePassword(TEST_EMAIL, passwordChangeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("New password and confirmation do not match");
        }

        @Test
        @DisplayName("Should not call repository when passwords don't match")
        void changePassword_PasswordsDontMatch_DoesNotCallRepository() {
            // Arrange
            passwordChangeRequest.setConfirmPassword("DifferentPassword123!");

            // Act
            try {
                userService.changePassword(TEST_EMAIL, passwordChangeRequest);
            } catch (IllegalArgumentException e) {
                // Expected
            }

            // Assert
            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void changePassword_UserNotFound_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.changePassword(TEST_EMAIL, passwordChangeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should throw exception when current password is incorrect")
        void changePassword_IncorrectCurrentPassword_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.changePassword(TEST_EMAIL, passwordChangeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Current password is incorrect");
        }

        @Test
        @DisplayName("Should not save user when current password is incorrect")
        void changePassword_IncorrectCurrentPassword_DoesNotSaveUser() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);

            // Act
            try {
                userService.changePassword(TEST_EMAIL, passwordChangeRequest);
            } catch (IllegalArgumentException e) {
                // Expected
            }

            // Assert
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when new password same as current")
        void changePassword_NewPasswordSameAsCurrent_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.changePassword(TEST_EMAIL, passwordChangeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("New password must be different from current password");
        }

        @Test
        @DisplayName("Should not encode when new password same as current")
        void changePassword_NewPasswordSameAsCurrent_DoesNotEncode() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);

            // Act
            try {
                userService.changePassword(TEST_EMAIL, passwordChangeRequest);
            } catch (IllegalArgumentException e) {
                // Expected
            }

            // Assert
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ============================================
    // CHANGE PASSWORD - EDGE CASES TESTS
    // ============================================

    @Nested
    @DisplayName("Change Password Edge Cases Tests")
    class ChangePasswordEdgeCasesTests {

        @Test
        @DisplayName("Should handle empty current password")
        void changePassword_EmptyCurrentPassword_ThrowsException() {
            // Arrange
            passwordChangeRequest.setCurrentPassword("");

            // Act & Assert - validation happens at request level but service should handle
            assertThatThrownBy(() -> userService.changePassword(TEST_EMAIL, passwordChangeRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle empty new password with correct matching")
        void changePassword_EmptyNewPassword_ValidatesAtServiceLevel() {
            // Arrange
            passwordChangeRequest.setNewPassword("");
            passwordChangeRequest.setConfirmPassword("");

            // Act & Assert
            // Since passwords match (both empty), we'll get to user lookup
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(TEST_EMAIL, passwordChangeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Current password is incorrect");
        }

        @Test
        @DisplayName("Should handle very long email")
        void changePassword_VeryLongEmail_HandlesCorrectly() {
            // Arrange
            String longEmail = "a".repeat(100) + "@gearup.com";
            when(userRepository.findByEmail(longEmail)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            PasswordChangeResponse response = userService.changePassword(longEmail, passwordChangeRequest);

            // Assert
            assertThat(response.getMessage()).isEqualTo("Password changed successfully");
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void changePassword_SpecialCharactersInPassword_Success() {
            // Arrange
            String specialPassword = "P@ssw0rd!@#$%^&*()";
            passwordChangeRequest.setNewPassword(specialPassword);
            passwordChangeRequest.setConfirmPassword(specialPassword);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(specialPassword, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(specialPassword)).thenReturn("encodedSpecialPassword");

            // Act
            PasswordChangeResponse response = userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            assertThat(response.getMessage()).isEqualTo("Password changed successfully");
            verify(passwordEncoder).encode(specialPassword);
        }

        @Test
        @DisplayName("Should handle whitespace in passwords")
        void changePassword_WhitespaceInPasswords_HandlesCorrectly() {
            // Arrange
            String passwordWithSpaces = "Pass word 123!";
            passwordChangeRequest.setNewPassword(passwordWithSpaces);
            passwordChangeRequest.setConfirmPassword(passwordWithSpaces);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(passwordWithSpaces, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(passwordWithSpaces)).thenReturn("encodedPasswordWithSpaces");

            // Act
            PasswordChangeResponse response = userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert
            assertThat(response.getMessage()).isEqualTo("Password changed successfully");
        }
    }

    // ============================================
    // REQUIRES PASSWORD CHANGE TESTS
    // ============================================

    @Nested
    @DisplayName("Requires Password Change Tests")
    class RequiresPasswordChangeTests {

        @Test
        @DisplayName("Should return true when user requires password change")
        void requiresPasswordChange_UserRequiresChange_ReturnsTrue() {
            // Arrange
            testUser.setRequiresPasswordChange(true);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            // Act
            boolean result = userService.requiresPasswordChange(TEST_EMAIL);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user does not require password change")
        void requiresPasswordChange_UserDoesNotRequireChange_ReturnsFalse() {
            // Arrange
            testUser.setRequiresPasswordChange(false);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            // Act
            boolean result = userService.requiresPasswordChange(TEST_EMAIL);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when requiresPasswordChange is null")
        void requiresPasswordChange_NullValue_ReturnsFalse() {
            // Arrange
            testUser.setRequiresPasswordChange(null);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            // Act
            boolean result = userService.requiresPasswordChange(TEST_EMAIL);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void requiresPasswordChange_UserNotFound_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.requiresPasswordChange(TEST_EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should handle different email formats")
        void requiresPasswordChange_DifferentEmailFormats_WorksCorrectly() {
            // Arrange
            String emailWithPlus = "test+tag@gearup.com";
            testUser.setRequiresPasswordChange(true);
            when(userRepository.findByEmail(emailWithPlus)).thenReturn(Optional.of(testUser));

            // Act
            boolean result = userService.requiresPasswordChange(emailWithPlus);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should call repository exactly once")
        void requiresPasswordChange_CallsRepository_ExactlyOnce() {
            // Arrange
            testUser.setRequiresPasswordChange(true);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            // Act
            userService.requiresPasswordChange(TEST_EMAIL);

            // Assert
            verify(userRepository, times(1)).findByEmail(TEST_EMAIL);
        }
    }

    // ============================================
    // INTEGRATION SCENARIO TESTS
    // ============================================

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("Should complete full password change workflow")
        void passwordChangeWorkflow_FullScenario_Success() {
            // Arrange - Employee with required password change
            User employee = UserFixtures.employeeUser();
            employee.setPassword(ENCODED_CURRENT_PASSWORD);
            employee.setRequiresPasswordChange(true);

            when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act - Check requires change before
            boolean requiresBefore = userService.requiresPasswordChange(employee.getEmail());
            
            // Change password
            PasswordChangeResponse response = userService.changePassword(employee.getEmail(), passwordChangeRequest);
            
            // Check requires change after
            when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
            boolean requiresAfter = userService.requiresPasswordChange(employee.getEmail());

            // Assert
            assertThat(requiresBefore).isTrue();
            assertThat(response.isRequiresPasswordChange()).isFalse();
            assertThat(requiresAfter).isFalse();
            assertThat(employee.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("Should handle customer password change without requirement")
        void passwordChangeWorkflow_CustomerWithoutRequirement_Success() {
            // Arrange - Customer without password change requirement
            User customer = UserFixtures.verifiedCustomer();
            customer.setPassword(ENCODED_CURRENT_PASSWORD);
            customer.setRequiresPasswordChange(false);

            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            boolean requiresBefore = userService.requiresPasswordChange(customer.getEmail());
            PasswordChangeResponse response = userService.changePassword(customer.getEmail(), passwordChangeRequest);

            // Assert
            assertThat(requiresBefore).isFalse();
            assertThat(response.isRequiresPasswordChange()).isFalse();
            assertThat(customer.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("Should maintain user data integrity after password change")
        void passwordChange_MaintainsUserData_Success() {
            // Arrange
            String originalEmail = testUser.getEmail();
            String originalName = testUser.getName();
            Boolean originalIsVerified = testUser.getIsVerified();
            Boolean originalIsActive = testUser.getIsActive();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

            // Act
            userService.changePassword(TEST_EMAIL, passwordChangeRequest);

            // Assert - Only password and requiresPasswordChange should change
            assertThat(testUser.getEmail()).isEqualTo(originalEmail);
            assertThat(testUser.getName()).isEqualTo(originalName);
            assertThat(testUser.getIsVerified()).isEqualTo(originalIsVerified);
            assertThat(testUser.getIsActive()).isEqualTo(originalIsActive);
            assertThat(testUser.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
            assertThat(testUser.getRequiresPasswordChange()).isFalse();
        }
    }
}

