package com.ead.gearup.integration.controller;

import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.EmailService;
import com.ead.gearup.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminController
 * Tests all admin-specific endpoints with Spring context loaded
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerIntegrationTest {

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

    private static final String BASE_URL = "/api/v1/admin";
    private static final String ADMIN_EMAIL = "admin@gearup.com";
    private static final String ADMIN_PASSWORD = "Admin@123456";
    private static final String TEST_PASSWORD = "Test@123456";

    @BeforeEach
    void setUp() {
        // Reset email service mock before each test
        // Note: @Transactional will auto-rollback database changes after each test
        reset(emailService);
    }

    // ==================== CHECK ADMIN INIT TESTS ====================

    @Test
    @DisplayName("GET /check-init - Should return admin exists when admin present with admin auth")
    void checkAdminInit_AdminExists_ReturnsTrue() throws Exception {
        // Arrange - Create admin user
        User admin = createAdminUser(ADMIN_EMAIL, ADMIN_PASSWORD, "System Admin");
        String adminToken = generateAdminToken(admin);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/check-init")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Admin account exists"))
                .andExpect(jsonPath("$.data.adminExists").value(true))
                .andExpect(jsonPath("$.data.adminEmail").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/check-init"));
    }

    @Test
    @DisplayName("GET /check-init - Should return admin not initialized with admin auth")
    void checkAdminInit_NoAdmin_ReturnsFalse() throws Exception {
        // Arrange - Create an admin to authenticate
        User admin = createAdminUser("temp@gearup.com", ADMIN_PASSWORD, "Temp Admin");
        String adminToken = generateAdminToken(admin);
        
        // Delete the admin@gearup.com if exists
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);

        // Act & Assert - No default admin created
        mockMvc.perform(get(BASE_URL + "/check-init")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Admin account not initialized"))
                .andExpect(jsonPath("$.data.adminExists").value(false))
                .andExpect(jsonPath("$.data.adminEmail").isEmpty());
    }

    @Test
    @DisplayName("GET /check-init - Should return 401 without authentication")
    void checkAdminInit_NoAuthentication_ReturnsUnauthorized() throws Exception {
        // Arrange
        createAdminUser(ADMIN_EMAIL, ADMIN_PASSWORD, "System Admin");

        // Act & Assert - No Bearer token provided, should get 401
        mockMvc.perform(get(BASE_URL + "/check-init"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /check-init - Should handle multiple admin users")
    void checkAdminInit_MultipleAdmins_StillReturnsDefaultAdmin() throws Exception {
        // Arrange - Create multiple admins
        User admin = createAdminUser(ADMIN_EMAIL, ADMIN_PASSWORD, "System Admin");
        createAdminUser("admin2@gearup.com", ADMIN_PASSWORD, "Second Admin");
        String adminToken = generateAdminToken(admin);

        // Act & Assert - Should still check for default admin email
        mockMvc.perform(get(BASE_URL + "/check-init")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminExists").value(true))
                .andExpect(jsonPath("$.data.adminEmail").value(ADMIN_EMAIL));
    }

    @Test
    @DisplayName("GET /check-init - Should return 403 when authenticated as CUSTOMER")
    void checkAdminInit_CustomerAuth_ReturnsForbidden() throws Exception {
        // Arrange
        User customer = createCustomerUser("customer@gearup.com", TEST_PASSWORD, "Test Customer");
        String customerToken = generateCustomerToken(customer);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/check-init")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /check-init - Should return 403 when authenticated as EMPLOYEE")
    void checkAdminInit_EmployeeAuth_ReturnsForbidden() throws Exception {
        // Arrange
        User employee = createEmployeeUser("employee@gearup.com", TEST_PASSWORD, "Test Employee");
        String employeeToken = generateEmployeeToken(employee);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/check-init")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    // ==================== CREATE ADMIN TESTS ====================

    @Test
    @DisplayName("POST /create-admin - Should create admin with valid data and authentication")
    void createAdmin_ValidDataWithAdminAuth_Success() throws Exception {
        // Arrange
        User admin = createAdminUser("creator@gearup.com", ADMIN_PASSWORD, "Creator Admin");
        String adminToken = generateAdminToken(admin);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Admin user created successfully"))
                .andExpect(jsonPath("$.data.email").value("newadmin@gearup.com"))
                .andExpect(jsonPath("$.data.name").value("New Admin"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/create-admin"));

        // Verify admin was created in database
        User savedAdmin = userRepository.findByEmail("newadmin@gearup.com").orElse(null);
        assertThat(savedAdmin).isNotNull();
        assertThat(savedAdmin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(savedAdmin.getIsVerified()).isTrue();
        assertThat(passwordEncoder.matches(TEST_PASSWORD, savedAdmin.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST /create-admin - Should return 403 when authenticated as CUSTOMER")
    void createAdmin_CustomerAuth_ReturnsForbidden() throws Exception {
        // Arrange
        User customer = createCustomerUser("customer@gearup.com", TEST_PASSWORD, "Test Customer");
        String customerToken = generateCustomerToken(customer);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"));

        // Verify admin was NOT created
        User admin = userRepository.findByEmail("newadmin@gearup.com").orElse(null);
        assertThat(admin).isNull();
    }

    @Test
    @DisplayName("POST /create-admin - Should return 403 when authenticated as EMPLOYEE")
    void createAdmin_EmployeeAuth_ReturnsForbidden() throws Exception {
        // Arrange
        User employee = createEmployeeUser("employee@gearup.com", TEST_PASSWORD, "Test Employee");
        String employeeToken = generateEmployeeToken(employee);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /create-admin - Should return 401 when not authenticated")
    void createAdmin_NoAuthentication_ReturnsUnauthorized() throws Exception {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /create-admin - Should return 400 when email already exists")
    void createAdmin_EmailExists_ReturnsBadRequest() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        // Create existing user
        createCustomerUser("existing@gearup.com", TEST_PASSWORD, "Existing User");

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("existing@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User with this email already exists"));
    }

    @Test
    @DisplayName("POST /create-admin - Should return 400 for invalid email")
    void createAdmin_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("invalid-email");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /create-admin - Should return 400 for weak password")
    void createAdmin_WeakPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword("weak");
        createDTO.setName("New Admin");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /create-admin - Should return 400 for missing required fields")
    void createAdmin_MissingFields_ReturnsBadRequest() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        UserCreateDTO createDTO = new UserCreateDTO();
        // Missing all fields

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /create-admin - Should auto-verify admin account")
    void createAdmin_ValidData_AutoVerifiesAdmin() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated());

        // Assert - Admin should be pre-verified
        User savedAdmin = userRepository.findByEmail("newadmin@gearup.com").orElse(null);
        assertThat(savedAdmin).isNotNull();
        assertThat(savedAdmin.getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("POST /create-admin - Should encode password before saving")
    void createAdmin_ValidData_EncodesPassword() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("New Admin");

        // Act
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated());

        // Assert - Password should be encoded
        User savedAdmin = userRepository.findByEmail("newadmin@gearup.com").orElse(null);
        assertThat(savedAdmin).isNotNull();
        assertThat(savedAdmin.getPassword()).isNotEqualTo(TEST_PASSWORD);
        assertThat(passwordEncoder.matches(TEST_PASSWORD, savedAdmin.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST /create-admin - Should handle special characters in name")
    void createAdmin_SpecialCharactersInName_Success() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("newadmin@gearup.com");
        createDTO.setPassword(TEST_PASSWORD);
        createDTO.setName("José María O'Brien-Smith");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("José María O'Brien-Smith"));
    }

    // ==================== MIGRATE CUSTOMERS TESTS ====================

    @Test
    @DisplayName("POST /migrate-customers - Should migrate customers with admin authentication")
    void migrateCustomers_AdminAuth_Success() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        // Create users with CUSTOMER role but no Customer entity
        User customer1 = createUserWithoutCustomerEntity("customer1@gearup.com", TEST_PASSWORD, "Customer One");
        User customer2 = createUserWithoutCustomerEntity("customer2@gearup.com", TEST_PASSWORD, "Customer Two");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("Migration completed")))
                .andExpect(jsonPath("$.message").value(containsString("2 Customer records")))
                .andExpect(jsonPath("$.data.createdRecords").value(2))
                .andExpect(jsonPath("$.data.totalUsersProcessed").value(2))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/migrate-customers"));

        // Verify Customer entities were created
        Customer savedCustomer1 = customerRepository.findByUser(customer1).orElse(null);
        Customer savedCustomer2 = customerRepository.findByUser(customer2).orElse(null);
        assertThat(savedCustomer1).isNotNull();
        assertThat(savedCustomer2).isNotNull();
    }

    @Test
    @DisplayName("POST /migrate-customers - Should return 403 when authenticated as CUSTOMER")
    void migrateCustomers_CustomerAuth_ReturnsForbidden() throws Exception {
        // Arrange
        User customer = createCustomerUser("customer@gearup.com", TEST_PASSWORD, "Test Customer");
        String customerToken = generateCustomerToken(customer);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /migrate-customers - Should return 403 when authenticated as EMPLOYEE")
    void migrateCustomers_EmployeeAuth_ReturnsForbidden() throws Exception {
        // Arrange
        User employee = createEmployeeUser("employee@gearup.com", TEST_PASSWORD, "Test Employee");
        String employeeToken = generateEmployeeToken(employee);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /migrate-customers - Should return 401 when not authenticated")
    void migrateCustomers_NoAuthentication_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/migrate-customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /migrate-customers - Should handle zero migrations when all customers exist")
    void migrateCustomers_AllCustomersExist_ReturnsZero() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        // Create customers with Customer entities already existing
        createCustomerUser("customer1@gearup.com", TEST_PASSWORD, "Customer One");
        createCustomerUser("customer2@gearup.com", TEST_PASSWORD, "Customer Two");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("0 Customer records")))
                .andExpect(jsonPath("$.data.createdRecords").value(0))
                .andExpect(jsonPath("$.data.totalUsersProcessed").value(0));
    }

    @Test
    @DisplayName("POST /migrate-customers - Should only migrate CUSTOMER role users")
    void migrateCustomers_OnlyCustomerRole_MigratesCorrectly() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        // Create various user types
        User customer = createUserWithoutCustomerEntity("customer@gearup.com", TEST_PASSWORD, "Customer");
        User employee = createEmployeeUser("employee@gearup.com", TEST_PASSWORD, "Employee");
        User anotherAdmin = createAdminUser("admin2@gearup.com", ADMIN_PASSWORD, "Admin 2");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdRecords").value(1)) // Only customer
                .andExpect(jsonPath("$.data.totalUsersProcessed").value(1));

        // Verify only customer got a Customer entity
        assertThat(customerRepository.findByUser(customer)).isPresent();
        assertThat(customerRepository.findByUser(employee)).isEmpty();
        assertThat(customerRepository.findByUser(anotherAdmin)).isEmpty();
    }

    @Test
    @DisplayName("POST /migrate-customers - Should handle large batch of customers")
    void migrateCustomers_LargeBatch_Success() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        // Create 10 customers without Customer entities
        for (int i = 1; i <= 10; i++) {
            createUserWithoutCustomerEntity("customer" + i + "@gearup.com", TEST_PASSWORD, "Customer " + i);
        }

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdRecords").value(10))
                .andExpect(jsonPath("$.data.totalUsersProcessed").value(10));

        // Verify all customers were migrated
        List<Customer> allCustomers = customerRepository.findAll();
        assertThat(allCustomers).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("POST /migrate-customers - Should set phoneNumber to null for migrated customers")
    void migrateCustomers_ValidData_SetsPhoneNumberNull() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        User customer = createUserWithoutCustomerEntity("customer@gearup.com", TEST_PASSWORD, "Customer");

        // Act
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Assert
        Customer savedCustomer = customerRepository.findByUser(customer).orElse(null);
        assertThat(savedCustomer).isNotNull();
        assertThat(savedCustomer.getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("POST /migrate-customers - Should not affect existing customers")
    void migrateCustomers_ExistingCustomers_NotAffected() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        // Create customer with existing Customer entity and phone number
        User existingCustomerUser = createCustomerUser("existing@gearup.com", TEST_PASSWORD, "Existing Customer");
        Customer existingCustomer = customerRepository.findByUser(existingCustomerUser).orElseThrow();
        existingCustomer.setPhoneNumber("1234567890");
        customerRepository.save(existingCustomer);

        // Create new customer without Customer entity
        createUserWithoutCustomerEntity("new@gearup.com", TEST_PASSWORD, "New Customer");

        // Act
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdRecords").value(1));

        // Assert - Existing customer should still have phone number
        Customer stillExistingCustomer = customerRepository.findByUser(existingCustomerUser).orElse(null);
        assertThat(stillExistingCustomer).isNotNull();
        assertThat(stillExistingCustomer.getPhoneNumber()).isEqualTo("1234567890");
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    @DisplayName("Integration - Admin creates another admin and new admin can migrate customers")
    void integrationScenario_AdminCreatesAdminAndMigrates_Success() throws Exception {
        // Step 1: Create first admin
        User firstAdmin = createAdminUser("admin1@gearup.com", ADMIN_PASSWORD, "First Admin");
        String firstAdminToken = generateAdminToken(firstAdmin);

        // Step 2: First admin creates second admin
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("admin2@gearup.com");
        createDTO.setPassword(ADMIN_PASSWORD);
        createDTO.setName("Second Admin");

        mockMvc.perform(post(BASE_URL + "/create-admin")
                        .header("Authorization", "Bearer " + firstAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated());

        // Step 3: Second admin logs in and creates customers to migrate
        User secondAdmin = userRepository.findByEmail("admin2@gearup.com").orElseThrow();
        String secondAdminToken = generateAdminToken(secondAdmin);

        // Create customers without Customer entities
        createUserWithoutCustomerEntity("customer1@gearup.com", TEST_PASSWORD, "Customer One");
        createUserWithoutCustomerEntity("customer2@gearup.com", TEST_PASSWORD, "Customer Two");

        // Step 4: Second admin migrates customers
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdRecords").value(2));
    }

    @Test
    @DisplayName("Integration - Check init before and after admin creation")
    void integrationScenario_CheckInitBeforeAndAfterCreation_Success() throws Exception {
        // Step 1: Create a temporary admin to authenticate
        User tempAdmin = createAdminUser("temp@gearup.com", ADMIN_PASSWORD, "Temp Admin");
        String adminToken = generateAdminToken(tempAdmin);

        // Step 2: Delete default admin if exists and check init
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);
        
        mockMvc.perform(get(BASE_URL + "/check-init")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminExists").value(false));

        // Step 3: Create default admin
        createAdminUser(ADMIN_EMAIL, ADMIN_PASSWORD, "System Admin");

        // Step 4: Check init again
        mockMvc.perform(get(BASE_URL + "/check-init")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminExists").value(true))
                .andExpect(jsonPath("$.data.adminEmail").value(ADMIN_EMAIL));
    }

    @Test
    @DisplayName("Integration - Multiple migrations are idempotent")
    void integrationScenario_MultipleMigrations_Idempotent() throws Exception {
        // Arrange
        User admin = createAdminUser("admin@gearup.com", ADMIN_PASSWORD, "Admin");
        String adminToken = generateAdminToken(admin);

        // Create customers without Customer entities
        createUserWithoutCustomerEntity("customer1@gearup.com", TEST_PASSWORD, "Customer One");
        createUserWithoutCustomerEntity("customer2@gearup.com", TEST_PASSWORD, "Customer Two");

        // First migration
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdRecords").value(2));

        // Second migration (should create nothing)
        mockMvc.perform(post(BASE_URL + "/migrate-customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdRecords").value(0))
                .andExpect(jsonPath("$.data.totalUsersProcessed").value(0));
    }

    // ==================== HELPER METHODS ====================

    private User createAdminUser(String email, String password, String name) {
        User admin = User.builder()
                .email(email.toLowerCase().trim())
                .name(name)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(admin);
    }

    private User createCustomerUser(String email, String password, String name) {
        User user = User.builder()
                .email(email.toLowerCase().trim())
                .name(name)
                .password(passwordEncoder.encode(password))
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Create Customer entity
        Customer customer = Customer.builder()
                .user(savedUser)
                .build();
        customerRepository.save(customer);

        return savedUser;
    }

    private User createEmployeeUser(String email, String password, String name) {
        User employee = User.builder()
                .email(email.toLowerCase().trim())
                .name(name)
                .password(passwordEncoder.encode(password))
                .role(UserRole.EMPLOYEE)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(employee);
    }

    private User createUserWithoutCustomerEntity(String email, String password, String name) {
        User user = User.builder()
                .email(email.toLowerCase().trim())
                .name(name)
                .password(passwordEncoder.encode(password))
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private String generateAdminToken(User admin) {
        UserPrinciple userPrinciple = new UserPrinciple(admin);
        return jwtService.generateAccessToken(userPrinciple);
    }

    private String generateCustomerToken(User customer) {
        UserPrinciple userPrinciple = new UserPrinciple(customer);
        return jwtService.generateAccessToken(userPrinciple);
    }

    private String generateEmployeeToken(User employee) {
        UserPrinciple userPrinciple = new UserPrinciple(employee);
        return jwtService.generateAccessToken(userPrinciple);
    }
}

