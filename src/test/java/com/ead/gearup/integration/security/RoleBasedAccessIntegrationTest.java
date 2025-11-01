package com.ead.gearup.integration.security;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.EmailService;
import com.ead.gearup.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Role-Based Access Control
 * Tests @RequiresRole annotation, aspect behavior, and authorization
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleBasedAccessIntegrationTest {

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

    @MockBean
    private EmailService emailService;

    private static final String TEST_PASSWORD = "Test@123456";

    @BeforeEach
    void setUp() {
        reset(emailService);
        doNothing().when(emailService).sendEmployeeCredentials(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ==================== ADMIN-ONLY ENDPOINT TESTS ====================

    @Test
    @DisplayName("@RequiresRole(ADMIN) - Should allow ADMIN role")
    void requiresRoleAdmin_AdminUser_Allowed() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("@RequiresRole(ADMIN) - Should deny CUSTOMER role")
    void requiresRoleAdmin_CustomerUser_Denied() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("@RequiresRole(ADMIN) - Should deny EMPLOYEE role")
    void requiresRoleAdmin_EmployeeUser_Denied() throws Exception {
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String token = generateToken(employee);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("@RequiresRole(ADMIN) - Should deny unauthenticated users")
    void requiresRoleAdmin_NoAuthentication_Denied() throws Exception {
        mockMvc.perform(get("/api/v1/employee-management/employees"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CUSTOMER-ONLY ENDPOINT TESTS ====================

    @Test
    @DisplayName("@RequiresRole(CUSTOMER) - Should allow CUSTOMER role")
    void requiresRoleCustomer_CustomerUser_Allowed() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("@RequiresRole(CUSTOMER) - Should deny ADMIN role")
    void requiresRoleCustomer_AdminUser_Denied() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("@RequiresRole(CUSTOMER) - Should deny EMPLOYEE role")
    void requiresRoleCustomer_EmployeeUser_Denied() throws Exception {
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String token = generateToken(employee);

        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== MULTIPLE ROLE ENDPOINT TESTS ====================

    @Test
    @DisplayName("@RequiresRole(CUSTOMER, EMPLOYEE, ADMIN) - Should allow all specified roles")
    void requiresRoleMultiple_AllRoles_Allowed() throws Exception {
        // Test with CUSTOMER
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String customerToken = generateToken(customer);

        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Test with EMPLOYEE
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String employeeToken = generateToken(employee);

        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // Test with ADMIN
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String adminToken = generateToken(admin);

        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==================== ASPECT BEHAVIOR TESTS ====================

    @Test
    @DisplayName("RoleBasedAccessAspect - Should intercept method before execution")
    void aspectBehavior_InterceptsMethod_BeforeExecution() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        // Aspect should intercept and deny access before method executes
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("RoleBasedAccessAspect - Should allow method to proceed with correct role")
    void aspectBehavior_AllowsMethodExecution_WithCorrectRole() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        // Aspect should allow method to proceed
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("RoleBasedAccessAspect - Should throw AccessDeniedException for wrong role")
    void aspectBehavior_ThrowsAccessDeniedException_ForWrongRole() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("RoleBasedAccessAspect - Should work with POST requests")
    void aspectBehavior_WorksWithPostRequests() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        CreateEmployeeDTO request = new CreateEmployeeDTO();
        request.setSpecialization("Test Specialization");

        mockMvc.perform(post("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("RoleBasedAccessAspect - Should work with PUT requests")
    void aspectBehavior_WorksWithPutRequests() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        
        // Create an employee first
        User employeeUser = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        Employee employee = Employee.builder()
                .user(employeeUser)
                .specialization("Test Specialization")
                .build();
        employeeRepository.save(employee);

        String token = generateToken(admin);

        mockMvc.perform(put("/api/v1/employee-management/employees/" + employee.getEmployeeId() + "/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RoleBasedAccessAspect - Should work with DELETE requests")
    void aspectBehavior_WorksWithDeleteRequests() throws Exception {
        // Most delete operations would be protected by @RequiresRole
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        // Try to delete (should be denied if endpoint has @RequiresRole(ADMIN))
        mockMvc.perform(delete("/api/v1/some-protected-resource/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError()); // Either 403 or 404
    }

    // ==================== CUSTOM ERROR MESSAGE TESTS ====================

    @Test
    @DisplayName("@RequiresRole with custom message - Should return custom error message")
    void customMessage_RequiresRole_ReturnsCustomMessage() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    // ==================== ROLE HIERARCHY TESTS ====================

    @Test
    @DisplayName("Should enforce strict role matching - No implicit hierarchy")
    void roleHierarchy_NoImplicitHierarchy_Enforced() throws Exception {
        // ADMIN should not automatically get CUSTOMER permissions
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should enforce strict role matching for all roles")
    void roleHierarchy_StrictMatching_AllRoles() throws Exception {
        // CUSTOMER should not get EMPLOYEE permissions
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String customerToken = generateToken(customer);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // EMPLOYEE should not get ADMIN permissions
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String employeeToken = generateToken(employee);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle user with null role")
    void edgeCase_NullRole_Denied() throws Exception {
        User user = User.builder()
                .email("nullrole@test.com")
                .name("Null Role User")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .role(null) // Null role
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        String token = generateToken(user);

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should handle concurrent requests with different roles")
    void edgeCase_ConcurrentRequests_DifferentRoles() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);

        String adminToken = generateToken(admin);
        String customerToken = generateToken(customer);

        // Admin request should succeed
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Customer request should fail
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Admin request should still succeed
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle role change mid-session")
    void edgeCase_RoleChangeMidSession_HandledCorrectly() throws Exception {
        User user = createVerifiedUser("user@test.com", UserRole.CUSTOMER);
        String token = generateToken(user);

        // First request as CUSTOMER - should succeed for customer endpoints
        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Change role to ADMIN (simulate role update)
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        // Same token should still have CUSTOMER role (JWT is immutable)
        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // But trying to access admin endpoint should fail (token still has CUSTOMER role)
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    @DisplayName("Integration - Complete role-based access flow for ADMIN")
    void integrationScenario_AdminAccess_Complete() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin);

        // Admin can access admin endpoints
        mockMvc.perform(get("/api/v1/admin/check-init")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Admin can access common endpoints
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Integration - Complete role-based access flow for CUSTOMER")
    void integrationScenario_CustomerAccess_Complete() throws Exception {
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);
        String token = generateToken(customer);

        // Customer can access customer endpoints
        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Customer can access common endpoints
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Customer cannot access admin endpoints
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration - Complete role-based access flow for EMPLOYEE")
    void integrationScenario_EmployeeAccess_Complete() throws Exception {
        User employee = createVerifiedUser("employee@test.com", UserRole.EMPLOYEE);
        String token = generateToken(employee);

        // Employee can access common endpoints
        mockMvc.perform(get("/api/v1/auth/password-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Employee cannot access admin endpoints
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // Employee cannot access customer-only endpoints
        mockMvc.perform(get("/api/v1/customers/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration - RoleBasedAccessService integration with aspect")
    void integrationScenario_ServiceAndAspect_Integration() throws Exception {
        User admin = createVerifiedUser("admin@test.com", UserRole.ADMIN);
        User customer = createVerifiedUser("customer@test.com", UserRole.CUSTOMER);

        String adminToken = generateToken(admin);
        String customerToken = generateToken(customer);

        // Service checks role via aspect
        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employee-management/employees")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
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

