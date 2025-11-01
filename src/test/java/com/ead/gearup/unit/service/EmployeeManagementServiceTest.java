package com.ead.gearup.unit.service;

import com.ead.gearup.dto.CreateEmployeeRequest;
import com.ead.gearup.dto.CreateEmployeeResponse;
import com.ead.gearup.dto.EmployeeDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.fixtures.UserFixtures;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.EmailService;
import com.ead.gearup.service.EmployeeManagementService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for EmployeeManagementService
 * Tests employee CRUD operations, password management, and email sending
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeManagementService Unit Tests")
class EmployeeManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmployeeManagementService employeeManagementService;

    private CreateEmployeeRequest createEmployeeRequest;
    private User existingEmployee;

    private static final String TEST_EMAIL = "employee@gearup.com";
    private static final String TEST_NAME = "John Mechanic";
    private static final String TEST_ROLE = "Technician";
    private static final String TEST_SPECIALIZATION = "Engine Specialist";

    @BeforeEach
    void setUp() {
        createEmployeeRequest = new CreateEmployeeRequest();
        createEmployeeRequest.setEmail(TEST_EMAIL);
        createEmployeeRequest.setName(TEST_NAME);
        createEmployeeRequest.setRole(TEST_ROLE);
        createEmployeeRequest.setSpecialization(TEST_SPECIALIZATION);

        existingEmployee = UserFixtures.employeeUser();
    }

    // ============================================
    // CREATE EMPLOYEE - SUCCESS TESTS
    // ============================================

    @Nested
    @DisplayName("Create Employee Success Tests")
    class CreateEmployeeSuccessTests {

        @Test
        @DisplayName("Should create employee with all required fields")
        void createEmployee_ValidRequest_Success() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            CreateEmployeeResponse response = employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getEmployee()).isNotNull();
            assertThat(response.getEmployee().getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getEmployee().getName()).isEqualTo(TEST_NAME);
            assertThat(response.getEmployee().getRole()).isEqualTo(TEST_ROLE);
            assertThat(response.getEmployee().getSpecialization()).isEqualTo(TEST_SPECIALIZATION);
            assertThat(response.getTemporaryPassword()).isNotNull();
            assertThat(response.getMessage()).contains("Employee account created");
        }

        @Test
        @DisplayName("Should generate temporary password with correct length")
        void createEmployee_GeneratesTemporaryPassword_CorrectLength() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            CreateEmployeeResponse response = employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            assertThat(response.getTemporaryPassword()).hasSize(12);
        }

        @Test
        @DisplayName("Should generate password with required complexity")
        void createEmployee_GeneratesPassword_MeetsComplexityRequirements() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            CreateEmployeeResponse response = employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            String password = response.getTemporaryPassword();
            assertThat(password).matches(".*[A-Z].*"); // Has uppercase
            assertThat(password).matches(".*[a-z].*"); // Has lowercase
            assertThat(password).matches(".*[0-9].*"); // Has digit
            assertThat(password).matches(".*[@#$].*"); // Has special char
        }

        @Test
        @DisplayName("Should set employee user role correctly")
        void createEmployee_SetsRole_AsEmployee() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getRole()).isEqualTo(UserRole.EMPLOYEE);
        }

        @Test
        @DisplayName("Should auto-verify employee account")
        void createEmployee_AutoVerifies_Employee() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getIsVerified()).isTrue();
        }

        @Test
        @DisplayName("Should set requiresPasswordChange flag to true")
        void createEmployee_SetsPasswordChangeRequired_True() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getRequiresPasswordChange()).isTrue();
        }

        @Test
        @DisplayName("Should set employee as active")
        void createEmployee_SetsEmployee_Active() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should encode temporary password before saving")
        void createEmployee_EncodesPassword_BeforeSaving() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            verify(passwordEncoder).encode(anyString());
        }

        @Test
        @DisplayName("Should send credentials email to employee")
        void createEmployee_SendsCredentialsEmail_Success() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            verify(emailService).sendEmployeeCredentials(
                    eq(TEST_EMAIL),
                    eq(TEST_NAME),
                    anyString(),
                    eq(TEST_ROLE),
                    eq(TEST_SPECIALIZATION)
            );
        }

        @Test
        @DisplayName("Should not fail if email sending fails")
        void createEmployee_EmailFails_ContinuesSuccessfully() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService)
                    .sendEmployeeCredentials(anyString(), anyString(), anyString(), anyString(), anyString());

            // Act & Assert - Should not throw
            assertThatCode(() -> employeeManagementService.createEmployee(createEmployeeRequest))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================
    // CREATE EMPLOYEE - VALIDATION FAILURE TESTS
    // ============================================

    @Nested
    @DisplayName("Create Employee Validation Tests")
    class CreateEmployeeValidationTests {

        @Test
        @DisplayName("Should throw exception when email already exists")
        void createEmployee_EmailExists_ThrowsException() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.createEmployee(createEmployeeRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("Should not save user when email exists")
        void createEmployee_EmailExists_DoesNotSaveUser() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            // Act
            try {
                employeeManagementService.createEmployee(createEmployeeRequest);
            } catch (RuntimeException e) {
                // Expected
            }

            // Assert
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should not send email when employee creation fails")
        void createEmployee_CreationFails_DoesNotSendEmail() {
            // Arrange
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            // Act
            try {
                employeeManagementService.createEmployee(createEmployeeRequest);
            } catch (RuntimeException e) {
                // Expected
            }

            // Assert
            verify(emailService, never()).sendEmployeeCredentials(anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    // ============================================
    // GET ALL EMPLOYEES TESTS
    // ============================================

    @Nested
    @DisplayName("Get All Employees Tests")
    class GetAllEmployeesTests {

        @Test
        @DisplayName("Should return list of all employees")
        void getAllEmployees_HasEmployees_ReturnsList() {
            // Arrange
            User employee1 = createEmployeeUser(1L, "emp1@gearup.com", "Employee One", true);
            User employee2 = createEmployeeUser(2L, "emp2@gearup.com", "Employee Two", true);
            when(userRepository.findByRole(UserRole.EMPLOYEE)).thenReturn(Arrays.asList(employee1, employee2));

            // Act
            List<EmployeeDTO> employees = employeeManagementService.getAllEmployees();

            // Assert
            assertThat(employees).hasSize(2);
            assertThat(employees.get(0).getEmail()).isEqualTo("emp1@gearup.com");
            assertThat(employees.get(1).getEmail()).isEqualTo("emp2@gearup.com");
        }

        @Test
        @DisplayName("Should return empty list when no employees")
        void getAllEmployees_NoEmployees_ReturnsEmptyList() {
            // Arrange
            when(userRepository.findByRole(UserRole.EMPLOYEE)).thenReturn(Arrays.asList());

            // Act
            List<EmployeeDTO> employees = employeeManagementService.getAllEmployees();

            // Assert
            assertThat(employees).isEmpty();
        }

        @Test
        @DisplayName("Should include active status in response")
        void getAllEmployees_IncludesActiveStatus() {
            // Arrange
            User activeEmployee = createEmployeeUser(1L, "active@gearup.com", "Active Employee", true);
            User inactiveEmployee = createEmployeeUser(2L, "inactive@gearup.com", "Inactive Employee", false);
            when(userRepository.findByRole(UserRole.EMPLOYEE)).thenReturn(Arrays.asList(activeEmployee, inactiveEmployee));

            // Act
            List<EmployeeDTO> employees = employeeManagementService.getAllEmployees();

            // Assert
            assertThat(employees.get(0).isActive()).isTrue();
            assertThat(employees.get(1).isActive()).isFalse();
        }

        @Test
        @DisplayName("Should map all employee fields correctly")
        void getAllEmployees_MapsFields_Correctly() {
            // Arrange
            User employee = createEmployeeUser(1L, "test@gearup.com", "Test Employee", true);
            when(userRepository.findByRole(UserRole.EMPLOYEE)).thenReturn(Arrays.asList(employee));

            // Act
            List<EmployeeDTO> employees = employeeManagementService.getAllEmployees();

            // Assert
            EmployeeDTO dto = employees.get(0);
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Test Employee");
            assertThat(dto.getEmail()).isEqualTo("test@gearup.com");
            assertThat(dto.getRole()).isEqualTo("Employee");
            assertThat(dto.getSpecialization()).isEqualTo("General");
            assertThat(dto.getCreatedAt()).isNotNull();
        }
    }

    // ============================================
    // DEACTIVATE EMPLOYEE TESTS
    // ============================================

    @Nested
    @DisplayName("Deactivate Employee Tests")
    class DeactivateEmployeeTests {

        @Test
        @DisplayName("Should deactivate employee successfully")
        void deactivateEmployee_ValidId_Success() {
            // Arrange
            existingEmployee.setIsActive(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));

            // Act
            employeeManagementService.deactivateEmployee(1L);

            // Assert
            assertThat(existingEmployee.getIsActive()).isFalse();
            verify(userRepository).save(existingEmployee);
        }

        @Test
        @DisplayName("Should throw exception when employee not found")
        void deactivateEmployee_NotFound_ThrowsException() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.deactivateEmployee(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Employee not found");
        }

        @Test
        @DisplayName("Should throw exception when user is not employee")
        void deactivateEmployee_NotEmployee_ThrowsException() {
            // Arrange
            User customer = UserFixtures.verifiedCustomer();
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.deactivateEmployee(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User is not an employee");
        }

        @Test
        @DisplayName("Should not save when user is not employee")
        void deactivateEmployee_NotEmployee_DoesNotSave() {
            // Arrange
            User customer = UserFixtures.verifiedCustomer();
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

            // Act
            try {
                employeeManagementService.deactivateEmployee(1L);
            } catch (RuntimeException e) {
                // Expected
            }

            // Assert
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle already inactive employee")
        void deactivateEmployee_AlreadyInactive_Success() {
            // Arrange
            existingEmployee.setIsActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));

            // Act
            employeeManagementService.deactivateEmployee(1L);

            // Assert
            assertThat(existingEmployee.getIsActive()).isFalse();
            verify(userRepository).save(existingEmployee);
        }
    }

    // ============================================
    // REACTIVATE EMPLOYEE TESTS
    // ============================================

    @Nested
    @DisplayName("Reactivate Employee Tests")
    class ReactivateEmployeeTests {

        @Test
        @DisplayName("Should reactivate employee successfully")
        void reactivateEmployee_ValidId_Success() {
            // Arrange
            existingEmployee.setIsActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));

            // Act
            employeeManagementService.reactivateEmployee(1L);

            // Assert
            assertThat(existingEmployee.getIsActive()).isTrue();
            verify(userRepository).save(existingEmployee);
        }

        @Test
        @DisplayName("Should throw exception when employee not found")
        void reactivateEmployee_NotFound_ThrowsException() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.reactivateEmployee(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Employee not found");
        }

        @Test
        @DisplayName("Should throw exception when user is not employee")
        void reactivateEmployee_NotEmployee_ThrowsException() {
            // Arrange
            User customer = UserFixtures.verifiedCustomer();
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.reactivateEmployee(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User is not an employee");
        }

        @Test
        @DisplayName("Should handle already active employee")
        void reactivateEmployee_AlreadyActive_Success() {
            // Arrange
            existingEmployee.setIsActive(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));

            // Act
            employeeManagementService.reactivateEmployee(1L);

            // Assert
            assertThat(existingEmployee.getIsActive()).isTrue();
            verify(userRepository).save(existingEmployee);
        }
    }

    // ============================================
    // RESEND TEMPORARY PASSWORD TESTS
    // ============================================

    @Nested
    @DisplayName("Resend Temporary Password Tests")
    class ResendTemporaryPasswordTests {

        @Test
        @DisplayName("Should generate and send new password successfully")
        void resendTemporaryPassword_ValidId_Success() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));
            when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");

            // Act
            employeeManagementService.resendTemporaryPassword(1L);

            // Assert
            verify(passwordEncoder).encode(anyString());
            verify(userRepository).save(existingEmployee);
            verify(emailService).sendEmployeePasswordReset(
                    eq(existingEmployee.getEmail()),
                    eq(existingEmployee.getName()),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should generate password with correct complexity")
        void resendTemporaryPassword_GeneratesComplexPassword() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));
            ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
            when(passwordEncoder.encode(passwordCaptor.capture())).thenReturn("encodedPassword");

            // Act
            employeeManagementService.resendTemporaryPassword(1L);

            // Assert
            String generatedPassword = passwordCaptor.getValue();
            assertThat(generatedPassword).hasSize(12);
            assertThat(generatedPassword).matches(".*[A-Z].*");
            assertThat(generatedPassword).matches(".*[a-z].*");
            assertThat(generatedPassword).matches(".*[0-9].*");
            assertThat(generatedPassword).matches(".*[@#$].*");
        }

        @Test
        @DisplayName("Should throw exception when employee not found")
        void resendTemporaryPassword_NotFound_ThrowsException() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.resendTemporaryPassword(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Employee not found");
        }

        @Test
        @DisplayName("Should throw exception when user is not employee")
        void resendTemporaryPassword_NotEmployee_ThrowsException() {
            // Arrange
            User customer = UserFixtures.verifiedCustomer();
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.resendTemporaryPassword(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User is not an employee");
        }

        @Test
        @DisplayName("Should throw exception when email sending fails")
        void resendTemporaryPassword_EmailFails_ThrowsException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService)
                    .sendEmployeePasswordReset(anyString(), anyString(), anyString());

            // Act & Assert
            assertThatThrownBy(() -> employeeManagementService.resendTemporaryPassword(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send password reset email");
        }

        @Test
        @DisplayName("Should save employee even if email fails")
        void resendTemporaryPassword_EmailFails_StillSavesEmployee() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService)
                    .sendEmployeePasswordReset(anyString(), anyString(), anyString());

            // Act
            try {
                employeeManagementService.resendTemporaryPassword(1L);
            } catch (RuntimeException e) {
                // Expected
            }

            // Assert - User should still be saved before email attempt
            verify(userRepository).save(existingEmployee);
        }
    }

    // ============================================
    // EDGE CASES AND INTEGRATION TESTS
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in employee name")
        void createEmployee_SpecialCharactersInName_Success() {
            // Arrange
            createEmployeeRequest.setName("José María O'Brien-González");
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            CreateEmployeeResponse response = employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            assertThat(response.getEmployee().getName()).isEqualTo("José María O'Brien-González");
        }

        @Test
        @DisplayName("Should handle different email formats")
        void createEmployee_DifferentEmailFormats_Success() {
            // Arrange
            createEmployeeRequest.setEmail("employee+tag@sub.gearup.co.uk");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            CreateEmployeeResponse response = employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            assertThat(response.getEmployee().getEmail()).isEqualTo("employee+tag@sub.gearup.co.uk");
        }

        @Test
        @DisplayName("Should handle null role gracefully")
        void createEmployee_NullRole_Success() {
            // Arrange
            createEmployeeRequest.setRole(null);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            CreateEmployeeResponse response = employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            assertThat(response.getEmployee().getRole()).isNull();
        }

        @Test
        @DisplayName("Should generate different passwords on multiple calls")
        void createEmployee_MultipleCalls_GeneratesDifferentPasswords() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // Act
            CreateEmployeeResponse response1 = employeeManagementService.createEmployee(createEmployeeRequest);
            createEmployeeRequest.setEmail("different@gearup.com");
            CreateEmployeeResponse response2 = employeeManagementService.createEmployee(createEmployeeRequest);

            // Assert
            assertThat(response1.getTemporaryPassword()).isNotEqualTo(response2.getTemporaryPassword());
        }

        @Test
        @DisplayName("Should maintain data integrity through deactivate-reactivate cycle")
        void employeeLifecycle_DeactivateReactivate_MaintainsIntegrity() {
            // Arrange
            existingEmployee.setIsActive(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));
            String originalName = existingEmployee.getName();
            String originalEmail = existingEmployee.getEmail();

            // Act - Deactivate
            employeeManagementService.deactivateEmployee(1L);
            boolean afterDeactivate = existingEmployee.getIsActive();

            // Reactivate
            employeeManagementService.reactivateEmployee(1L);
            boolean afterReactivate = existingEmployee.getIsActive();

            // Assert
            assertThat(afterDeactivate).isFalse();
            assertThat(afterReactivate).isTrue();
            assertThat(existingEmployee.getName()).isEqualTo(originalName);
            assertThat(existingEmployee.getEmail()).isEqualTo(originalEmail);
        }
    }

    // Helper method to create test employee users
    private User createEmployeeUser(Long id, String email, String name, boolean isActive) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setName(name);
        user.setRole(UserRole.EMPLOYEE);
        user.setIsActive(isActive);
        user.setIsVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}

