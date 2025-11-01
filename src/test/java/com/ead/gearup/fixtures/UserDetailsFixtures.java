package com.ead.gearup.fixtures;

import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.enums.UserRole;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;

/**
 * Test fixtures for UserDetails objects
 */
public class UserDetailsFixtures {

    /**
     * Creates a UserDetails for a customer user
     */
    public static UserDetails customerUserDetails() {
        User user = User.builder()
                .userId(1L)
                .email("customer@gearup.com")
                .name("Test Customer")
                .password("encodedPassword")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        return new UserPrinciple(user);
    }

    /**
     * Creates a UserDetails for an employee user
     */
    public static UserDetails employeeUserDetails() {
        User user = User.builder()
                .userId(2L)
                .email("employee@gearup.com")
                .name("Test Employee")
                .password("encodedPassword")
                .role(UserRole.EMPLOYEE)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        return new UserPrinciple(user);
    }

    /**
     * Creates a UserDetails for an admin user
     */
    public static UserDetails adminUserDetails() {
        User user = User.builder()
                .userId(3L)
                .email("admin@gearup.com")
                .name("Test Admin")
                .password("encodedPassword")
                .role(UserRole.ADMIN)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        return new UserPrinciple(user);
    }

    /**
     * Creates a UserDetails with a specific email
     */
    public static UserDetails withEmail(String email) {
        User user = User.builder()
                .userId(4L)
                .email(email)
                .name("Test User")
                .password("encodedPassword")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        return new UserPrinciple(user);
    }

    /**
     * Creates a UserDetails for a public/unauthenticated user
     */
    public static UserDetails publicUserDetails() {
        User user = User.builder()
                .userId(5L)
                .email("public@gearup.com")
                .name("Public User")
                .password("encodedPassword")
                .role(UserRole.PUBLIC)
                .isVerified(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        return new UserPrinciple(user);
    }
}

