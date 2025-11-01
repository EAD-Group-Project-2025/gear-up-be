package com.ead.gearup.unit.service.auth;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.EmailNotVerifiedException;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomUserDetailsService
 * Tests UserDetails loading, verification checks, and exception handling
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User verifiedUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        verifiedUser = User.builder()
                .userId(1L)
                .email("verified@test.com")
                .name("Verified User")
                .password("encoded_password")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        unverifiedUser = User.builder()
                .userId(2L)
                .email("unverified@test.com")
                .name("Unverified User")
                .password("encoded_password")
                .role(UserRole.CUSTOMER)
                .isVerified(false)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== LOAD USER BY USERNAME TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Should load verified user successfully")
    void loadUserByUsername_VerifiedUser_ReturnsUserDetails() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("verified@test.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(UserPrinciple.class);
        assertThat(userDetails.getUsername()).isEqualTo("verified@test.com");
        assertThat(userDetails.getPassword()).isEqualTo("encoded_password");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        verify(userRepository, times(1)).findByEmail("verified@test.com");
    }

    @Test
    @DisplayName("loadUserByUsername - Should return UserDetails with correct authorities")
    void loadUserByUsername_VerifiedUser_ReturnsCorrectAuthorities() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("verified@test.com");

        // Assert
        assertThat(userDetails.getAuthorities()).isNotEmpty();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("loadUserByUsername - Should throw UsernameNotFoundException when user not found")
    void loadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: nonexistent@test.com");

        verify(userRepository, times(1)).findByEmail("nonexistent@test.com");
    }

    @Test
    @DisplayName("loadUserByUsername - Should throw EmailNotVerifiedException for unverified user")
    void loadUserByUsername_UnverifiedUser_ThrowsEmailNotVerifiedException() {
        // Arrange
        when(userRepository.findByEmail("unverified@test.com")).thenReturn(Optional.of(unverifiedUser));

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unverified@test.com"))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessage("Email not verified");

        verify(userRepository, times(1)).findByEmail("unverified@test.com");
    }

    @Test
    @DisplayName("loadUserByUsername - Should return UserPrinciple with correct userId")
    void loadUserByUsername_VerifiedUser_ReturnsUserPrincipleWithUserId() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("verified@test.com");
        UserPrinciple userPrinciple = (UserPrinciple) userDetails;

        // Assert
        assertThat(userPrinciple.getUserId()).isEqualTo(1L);
        assertThat(userPrinciple.getUser()).isEqualTo(verifiedUser);
    }

    // ==================== ROLE HANDLING TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Should load ADMIN user correctly")
    void loadUserByUsername_AdminUser_ReturnsCorrectRole() {
        // Arrange
        User adminUser = User.builder()
                .userId(3L)
                .email("admin@test.com")
                .name("Admin User")
                .password("encoded_password")
                .role(UserRole.ADMIN)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@test.com");

        // Assert
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername - Should load EMPLOYEE user correctly")
    void loadUserByUsername_EmployeeUser_ReturnsCorrectRole() {
        // Arrange
        User employeeUser = User.builder()
                .userId(4L)
                .email("employee@test.com")
                .name("Employee User")
                .password("encoded_password")
                .role(UserRole.EMPLOYEE)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("employee@test.com")).thenReturn(Optional.of(employeeUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("employee@test.com");

        // Assert
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("loadUserByUsername - Should load CUSTOMER user correctly")
    void loadUserByUsername_CustomerUser_ReturnsCorrectRole() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("verified@test.com");

        // Assert
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_CUSTOMER");
    }

    // ==================== EMAIL HANDLING TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Should handle email case sensitivity correctly")
    void loadUserByUsername_EmailCase_HandlesCorrectly() {
        // Arrange
        when(userRepository.findByEmail("VERIFIED@TEST.COM")).thenReturn(Optional.of(verifiedUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("VERIFIED@TEST.COM");

        // Assert
        assertThat(userDetails).isNotNull();
        verify(userRepository, times(1)).findByEmail("VERIFIED@TEST.COM");
    }

    @Test
    @DisplayName("loadUserByUsername - Should handle email with special characters")
    void loadUserByUsername_EmailWithSpecialChars_HandlesCorrectly() {
        // Arrange
        User specialUser = User.builder()
                .userId(5L)
                .email("user+tag@test.com")
                .name("Special User")
                .password("encoded_password")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("user+tag@test.com")).thenReturn(Optional.of(specialUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("user+tag@test.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("user+tag@test.com");
    }

    // ==================== VERIFICATION STATUS TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Should not allow login for user with isVerified=false")
    void loadUserByUsername_IsVerifiedFalse_ThrowsException() {
        // Arrange
        User userWithFalseVerification = User.builder()
                .userId(6L)
                .email("false@test.com")
                .name("False Verified")
                .password("encoded_password")
                .role(UserRole.CUSTOMER)
                .isVerified(false)  // Explicitly false
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("false@test.com")).thenReturn(Optional.of(userWithFalseVerification));

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("false@test.com"))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessage("Email not verified");
    }

    @Test
    @DisplayName("loadUserByUsername - Should allow login for user with isVerified=true")
    void loadUserByUsername_IsVerifiedTrue_Success() {
        // Arrange
        User userWithTrueVerification = User.builder()
                .userId(7L)
                .email("true@test.com")
                .name("True Verified")
                .password("encoded_password")
                .role(UserRole.CUSTOMER)
                .isVerified(true)  // Explicitly true
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("true@test.com")).thenReturn(Optional.of(userWithTrueVerification));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("true@test.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("true@test.com");
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("loadUserByUsername - Should handle null email gracefully")
    void loadUserByUsername_NullEmail_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository, times(1)).findByEmail(null);
    }

    @Test
    @DisplayName("loadUserByUsername - Should handle empty email string")
    void loadUserByUsername_EmptyEmail_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: ");

        verify(userRepository, times(1)).findByEmail("");
    }

    @Test
    @DisplayName("loadUserByUsername - Should handle very long email addresses")
    void loadUserByUsername_VeryLongEmail_HandlesCorrectly() {
        // Arrange
        String longEmail = "a".repeat(50) + "@" + "b".repeat(50) + ".com";
        User userWithLongEmail = User.builder()
                .userId(8L)
                .email(longEmail)
                .name("Long Email User")
                .password("encoded_password")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(longEmail)).thenReturn(Optional.of(userWithLongEmail));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(longEmail);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(longEmail);
    }

    // ==================== ACCOUNT STATUS TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Should return account with all status flags as true")
    void loadUserByUsername_VerifiedUser_AllStatusFlagsTrue() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("verified@test.com");

        // Assert
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername - Should load user even if requiresPasswordChange is true")
    void loadUserByUsername_RequiresPasswordChange_StillLoadsUser() {
        // Arrange
        User userRequiringPasswordChange = User.builder()
                .userId(9L)
                .email("needspasswordchange@test.com")
                .name("Needs Password Change")
                .password("encoded_password")
                .role(UserRole.EMPLOYEE)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(true)  // Should still allow loading
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("needspasswordchange@test.com"))
                .thenReturn(Optional.of(userRequiringPasswordChange));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("needspasswordchange@test.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("needspasswordchange@test.com");
    }

    // ==================== MULTIPLE CALLS TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Should handle multiple calls for same user")
    void loadUserByUsername_MultipleCalls_HandlesCorrectly() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        UserDetails firstCall = customUserDetailsService.loadUserByUsername("verified@test.com");
        UserDetails secondCall = customUserDetailsService.loadUserByUsername("verified@test.com");
        UserDetails thirdCall = customUserDetailsService.loadUserByUsername("verified@test.com");

        // Assert
        assertThat(firstCall).isNotNull();
        assertThat(secondCall).isNotNull();
        assertThat(thirdCall).isNotNull();
        assertThat(firstCall.getUsername()).isEqualTo(secondCall.getUsername());
        assertThat(secondCall.getUsername()).isEqualTo(thirdCall.getUsername());

        verify(userRepository, times(3)).findByEmail("verified@test.com");
    }

    @Test
    @DisplayName("loadUserByUsername - Should handle concurrent different user lookups")
    void loadUserByUsername_DifferentUsers_HandlesCorrectly() {
        // Arrange
        User user1 = verifiedUser;
        User user2 = User.builder()
                .userId(10L)
                .email("user2@test.com")
                .name("User Two")
                .password("password2")
                .role(UserRole.ADMIN)
                .isVerified(true)
                .isActive(true)
                .requiresPasswordChange(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(user2));

        // Act
        UserDetails details1 = customUserDetailsService.loadUserByUsername("verified@test.com");
        UserDetails details2 = customUserDetailsService.loadUserByUsername("user2@test.com");

        // Assert
        assertThat(details1.getUsername()).isEqualTo("verified@test.com");
        assertThat(details2.getUsername()).isEqualTo("user2@test.com");
        assertThat(details1.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_CUSTOMER");
        assertThat(details2.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    // ==================== REPOSITORY INTERACTION TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Should call repository exactly once per call")
    void loadUserByUsername_RepositoryCalled_OncePerCall() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        customUserDetailsService.loadUserByUsername("verified@test.com");

        // Assert
        verify(userRepository, times(1)).findByEmail("verified@test.com");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername - Should not call repository save")
    void loadUserByUsername_NoRepositorySave_CalledDuringLoad() {
        // Arrange
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));

        // Act
        customUserDetailsService.loadUserByUsername("verified@test.com");

        // Assert
        verify(userRepository, times(1)).findByEmail("verified@test.com");
        verify(userRepository, never()).save(any(User.class));
    }
}

