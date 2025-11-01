package com.ead.gearup.fixtures;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.User;

import java.time.LocalDateTime;

/**
 * Test data fixtures for User entity
 * Provides consistent test data across all test classes
 */
public class UserFixtures {

    // Common test data
    public static final String TEST_EMAIL = "test@gearup.com";
    public static final String TEST_PASSWORD = "Test@123";
    public static final String TEST_NAME = "Test User";
    public static final String ENCODED_PASSWORD = "encodedPassword123";

    /**
     * Creates a verified customer user for testing
     */
    public static User verifiedCustomer() {
        return User.builder()
                .userId(1L)
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .password(ENCODED_PASSWORD)
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an unverified customer user for testing
     */
    public static User unverifiedCustomer() {
        return User.builder()
                .userId(2L)
                .email("unverified@gearup.com")
                .name("Unverified User")
                .password(ENCODED_PASSWORD)
                .role(UserRole.CUSTOMER)
                .isVerified(false)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an employee user for testing
     */
    public static User employeeUser() {
        return User.builder()
                .userId(3L)
                .email("employee@gearup.com")
                .name("Test Employee")
                .password(ENCODED_PASSWORD)
                .role(UserRole.EMPLOYEE)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an admin user for testing
     */
    public static User adminUser() {
        return User.builder()
                .userId(4L)
                .email("admin@gearup.com")
                .name("Test Admin")
                .password(ENCODED_PASSWORD)
                .role(UserRole.ADMIN)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an inactive user for testing
     */
    public static User inactiveUser() {
        return User.builder()
                .userId(5L)
                .email("inactive@gearup.com")
                .name("Inactive User")
                .password(ENCODED_PASSWORD)
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(false)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a user with custom email
     */
    public static User withEmail(String email) {
        return User.builder()
                .userId(6L)
                .email(email)
                .name(TEST_NAME)
                .password(ENCODED_PASSWORD)
                .role(UserRole.CUSTOMER)
                .isVerified(false)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a user with last verification email sent timestamp
     */
    public static User withRecentVerificationEmail() {
        return User.builder()
                .userId(7L)
                .email("recent@gearup.com")
                .name(TEST_NAME)
                .password(ENCODED_PASSWORD)
                .role(UserRole.CUSTOMER)
                .isVerified(false)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .lastVerificationEmailSent(LocalDateTime.now().minusMinutes(2))
                .build();
    }

    /**
     * Creates a user with old verification email timestamp (beyond cooldown)
     */
    public static User withOldVerificationEmail() {
        return User.builder()
                .userId(8L)
                .email("old@gearup.com")
                .name(TEST_NAME)
                .password(ENCODED_PASSWORD)
                .role(UserRole.CUSTOMER)
                .isVerified(false)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .lastVerificationEmailSent(LocalDateTime.now().minusMinutes(10))
                .build();
    }
}

