package com.ead.gearup.fixtures;

import com.ead.gearup.dto.request.ResendEmailRequestDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;

/**
 * Test data fixtures for DTO objects
 */
public class DTOFixtures {

    public static final String VALID_EMAIL = "test@gearup.com";
    public static final String VALID_PASSWORD = "Test@123";
    public static final String VALID_NAME = "Test User";

    /**
     * Creates a valid UserCreateDTO for testing
     */
    public static UserCreateDTO validUserCreateDTO() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail(VALID_EMAIL);
        dto.setPassword(VALID_PASSWORD);
        dto.setName(VALID_NAME);
        return dto;
    }

    /**
     * Creates a UserCreateDTO with uppercase email
     */
    public static UserCreateDTO userCreateDTOWithUppercaseEmail() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("TEST@GEARUP.COM");
        dto.setPassword(VALID_PASSWORD);
        dto.setName(VALID_NAME);
        return dto;
    }

    /**
     * Creates a UserCreateDTO with whitespace in email
     */
    public static UserCreateDTO userCreateDTOWithWhitespaceEmail() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("  test@gearup.com  ");
        dto.setPassword(VALID_PASSWORD);
        dto.setName(VALID_NAME);
        return dto;
    }

    /**
     * Creates a valid UserLoginDTO
     */
    public static UserLoginDTO validUserLoginDTO() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(VALID_EMAIL);
        dto.setPassword(VALID_PASSWORD);
        return dto;
    }

    /**
     * Creates a UserLoginDTO with wrong password
     */
    public static UserLoginDTO userLoginDTOWithWrongPassword() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(VALID_EMAIL);
        dto.setPassword("WrongPassword@123");
        return dto;
    }

    /**
     * Creates a valid ResendEmailRequestDTO
     */
    public static ResendEmailRequestDTO validResendEmailRequestDTO() {
        ResendEmailRequestDTO dto = new ResendEmailRequestDTO();
        dto.setEmail(VALID_EMAIL);
        return dto;
    }

    /**
     * Creates a ResendEmailRequestDTO with uppercase email
     */
    public static ResendEmailRequestDTO resendEmailRequestDTOWithUppercase() {
        ResendEmailRequestDTO dto = new ResendEmailRequestDTO();
        dto.setEmail("TEST@GEARUP.COM");
        return dto;
    }
}

