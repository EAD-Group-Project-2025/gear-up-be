package com.ead.gearup.unit.service.auth;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.AccessDeniedException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.User;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.service.auth.RoleBasedAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoleBasedAccessService
 * Tests role checking, authentication handling, and database role retrieval
 */
@ExtendWith(MockitoExtension.class)
class RoleBasedAccessServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RoleBasedAccessService roleBasedAccessService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @BeforeEach
    void setUp() {
        // Mock SecurityContextHolder static methods
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        // Close the static mock
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }

    // ==================== GET CURRENT USER ROLE TESTS ====================

    @Test
    @DisplayName("getCurrentUserRole - Should return ADMIN role from authentication")
    void getCurrentUserRole_AdminAuthenticated_ReturnsAdminRole() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("getCurrentUserRole - Should return CUSTOMER role from authentication")
    void getCurrentUserRole_CustomerAuthenticated_ReturnsCustomerRole() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("getCurrentUserRole - Should return EMPLOYEE role from authentication")
    void getCurrentUserRole_EmployeeAuthenticated_ReturnsEmployeeRole() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role).isEqualTo(UserRole.EMPLOYEE);
    }

    @Test
    @DisplayName("getCurrentUserRole - Should return null when authentication is null")
    void getCurrentUserRole_NoAuthentication_ReturnsNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("getCurrentUserRole - Should return null when user is not authenticated")
    void getCurrentUserRole_NotAuthenticated_ReturnsNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("getCurrentUserRole - Should return null when no authorities present")
    void getCurrentUserRole_NoAuthorities_ReturnsNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn(List.of());

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("getCurrentUserRole - Should strip ROLE_ prefix correctly")
    void getCurrentUserRole_WithRolePrefix_StripsPrefix() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role).isEqualTo(UserRole.ADMIN);
        assertThat(role.name()).doesNotContain("ROLE_");
    }

    // ==================== HAS ROLE TESTS ====================

    @Test
    @DisplayName("hasRole - Should return true when user has required role")
    void hasRole_UserHasRole_ReturnsTrue() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasRole = roleBasedAccessService.hasRole(UserRole.ADMIN);

        // Assert
        assertThat(hasRole).isTrue();
    }

    @Test
    @DisplayName("hasRole - Should return false when user does not have required role")
    void hasRole_UserDoesNotHaveRole_ReturnsFalse() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasRole = roleBasedAccessService.hasRole(UserRole.ADMIN);

        // Assert
        assertThat(hasRole).isFalse();
    }

    @Test
    @DisplayName("hasRole - Should return false when current role is null")
    void hasRole_CurrentRoleNull_ReturnsFalse() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        boolean hasRole = roleBasedAccessService.hasRole(UserRole.ADMIN);

        // Assert
        assertThat(hasRole).isFalse();
    }

    @Test
    @DisplayName("hasRole - Should check CUSTOMER role correctly")
    void hasRole_CheckCustomerRole_WorksCorrectly() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasCustomer = roleBasedAccessService.hasRole(UserRole.CUSTOMER);
        boolean hasAdmin = roleBasedAccessService.hasRole(UserRole.ADMIN);
        boolean hasEmployee = roleBasedAccessService.hasRole(UserRole.EMPLOYEE);

        // Assert
        assertThat(hasCustomer).isTrue();
        assertThat(hasAdmin).isFalse();
        assertThat(hasEmployee).isFalse();
    }

    @Test
    @DisplayName("hasRole - Should check EMPLOYEE role correctly")
    void hasRole_CheckEmployeeRole_WorksCorrectly() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasEmployee = roleBasedAccessService.hasRole(UserRole.EMPLOYEE);
        boolean hasAdmin = roleBasedAccessService.hasRole(UserRole.ADMIN);
        boolean hasCustomer = roleBasedAccessService.hasRole(UserRole.CUSTOMER);

        // Assert
        assertThat(hasEmployee).isTrue();
        assertThat(hasAdmin).isFalse();
        assertThat(hasCustomer).isFalse();
    }

    // ==================== HAS ANY ROLE TESTS ====================

    @Test
    @DisplayName("hasAnyRole - Should return true when user has one of the required roles")
    void hasAnyRole_UserHasOneRole_ReturnsTrue() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasAnyRole = roleBasedAccessService.hasAnyRole(UserRole.ADMIN, UserRole.EMPLOYEE);

        // Assert
        assertThat(hasAnyRole).isTrue();
    }

    @Test
    @DisplayName("hasAnyRole - Should return false when user has none of the required roles")
    void hasAnyRole_UserHasNoneOfRoles_ReturnsFalse() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasAnyRole = roleBasedAccessService.hasAnyRole(UserRole.ADMIN, UserRole.EMPLOYEE);

        // Assert
        assertThat(hasAnyRole).isFalse();
    }

    @Test
    @DisplayName("hasAnyRole - Should return false when current role is null")
    void hasAnyRole_CurrentRoleNull_ReturnsFalse() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        boolean hasAnyRole = roleBasedAccessService.hasAnyRole(UserRole.ADMIN, UserRole.CUSTOMER);

        // Assert
        assertThat(hasAnyRole).isFalse();
    }

    @Test
    @DisplayName("hasAnyRole - Should handle single role in varargs")
    void hasAnyRole_SingleRole_WorksCorrectly() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasAnyRole = roleBasedAccessService.hasAnyRole(UserRole.ADMIN);

        // Assert
        assertThat(hasAnyRole).isTrue();
    }

    @Test
    @DisplayName("hasAnyRole - Should handle multiple roles including all role types")
    void hasAnyRole_MultipleRoles_WorksCorrectly() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasAnyRole = roleBasedAccessService.hasAnyRole(
                UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.CUSTOMER, UserRole.PUBLIC
        );

        // Assert
        assertThat(hasAnyRole).isTrue();
    }

    @Test
    @DisplayName("hasAnyRole - Should return false when checking PUBLIC role with CUSTOMER user")
    void hasAnyRole_PublicRoleCheck_WorksCorrectly() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasPublicRole = roleBasedAccessService.hasAnyRole(UserRole.PUBLIC);

        // Assert
        assertThat(hasPublicRole).isFalse();
    }

    // ==================== GET CURRENT USER ROLE FROM DATABASE TESTS ====================

    @Test
    @DisplayName("getCurrentUserRoleFromDatabase - Should return role from database")
    void getCurrentUserRoleFromDatabase_Success_ReturnsRole() {
        // Arrange
        User user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .role(UserRole.ADMIN)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(role).isEqualTo(UserRole.ADMIN);
        verify(currentUserService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("getCurrentUserRoleFromDatabase - Should return null when user is null")
    void getCurrentUserRoleFromDatabase_UserNull_ReturnsNull() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(null);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("getCurrentUserRoleFromDatabase - Should return null when exception occurs")
    void getCurrentUserRoleFromDatabase_Exception_ReturnsNull() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenThrow(new UserNotFoundException("User not found"));

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("getCurrentUserRoleFromDatabase - Should return null when AccessDeniedException occurs")
    void getCurrentUserRoleFromDatabase_AccessDenied_ReturnsNull() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenThrow(new AccessDeniedException("Unauthorized"));

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("getCurrentUserRoleFromDatabase - Should return CUSTOMER role")
    void getCurrentUserRoleFromDatabase_CustomerRole_ReturnsCustomer() {
        // Arrange
        User user = User.builder()
                .userId(2L)
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(role).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("getCurrentUserRoleFromDatabase - Should return EMPLOYEE role")
    void getCurrentUserRoleFromDatabase_EmployeeRole_ReturnsEmployee() {
        // Arrange
        User user = User.builder()
                .userId(3L)
                .email("employee@test.com")
                .role(UserRole.EMPLOYEE)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);

        // Act
        UserRole role = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(role).isEqualTo(UserRole.EMPLOYEE);
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    @DisplayName("Integration - getCurrentUserRole and hasRole should work together")
    void integration_GetRoleAndHasRole_WorkTogether() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        UserRole currentRole = roleBasedAccessService.getCurrentUserRole();
        boolean hasAdminRole = roleBasedAccessService.hasRole(UserRole.ADMIN);
        boolean hasCustomerRole = roleBasedAccessService.hasRole(UserRole.CUSTOMER);

        // Assert
        assertThat(currentRole).isEqualTo(UserRole.ADMIN);
        assertThat(hasAdminRole).isTrue();
        assertThat(hasCustomerRole).isFalse();
    }

    @Test
    @DisplayName("Integration - hasRole and hasAnyRole should be consistent")
    void integration_HasRoleAndHasAnyRole_Consistent() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasSingleRole = roleBasedAccessService.hasRole(UserRole.CUSTOMER);
        boolean hasAnyRole = roleBasedAccessService.hasAnyRole(UserRole.CUSTOMER);
        boolean hasAnyMultipleRoles = roleBasedAccessService.hasAnyRole(UserRole.CUSTOMER, UserRole.ADMIN);

        // Assert
        assertThat(hasSingleRole).isTrue();
        assertThat(hasAnyRole).isTrue();
        assertThat(hasAnyMultipleRoles).isTrue();
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge case - Multiple calls should return consistent results")
    void edgeCase_MultipleCalls_ConsistentResults() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        UserRole role1 = roleBasedAccessService.getCurrentUserRole();
        UserRole role2 = roleBasedAccessService.getCurrentUserRole();
        UserRole role3 = roleBasedAccessService.getCurrentUserRole();

        // Assert
        assertThat(role1).isEqualTo(UserRole.ADMIN);
        assertThat(role2).isEqualTo(UserRole.ADMIN);
        assertThat(role3).isEqualTo(UserRole.ADMIN);
        assertThat(role1).isEqualTo(role2).isEqualTo(role3);
    }

    @Test
    @DisplayName("Edge case - hasAnyRole with empty array should return false")
    void edgeCase_HasAnyRoleEmptyArray_ReturnsFalse() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean hasAnyRole = roleBasedAccessService.hasAnyRole();

        // Assert
        assertThat(hasAnyRole).isFalse();
    }

    @Test
    @DisplayName("Edge case - getCurrentUserRoleFromDatabase should not affect SecurityContext")
    void edgeCase_DatabaseRoleCheck_DoesNotAffectSecurityContext() {
        // Arrange
        User user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);

        // Act
        UserRole dbRole = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(dbRole).isEqualTo(UserRole.CUSTOMER);
        // Verify SecurityContext was not accessed
        verifyNoInteractions(securityContext);
        verifyNoInteractions(authentication);
    }

    @Test
    @DisplayName("Edge case - Roles from authentication and database can differ")
    void edgeCase_AuthRoleAndDbRole_CanDiffer() {
        // Arrange - Auth says ADMIN
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // DB says CUSTOMER (could happen with stale token)
        User user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .role(UserRole.CUSTOMER)
                .isVerified(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(user);

        // Act
        UserRole authRole = roleBasedAccessService.getCurrentUserRole();
        UserRole dbRole = roleBasedAccessService.getCurrentUserRoleFromDatabase();

        // Assert
        assertThat(authRole).isEqualTo(UserRole.ADMIN);
        assertThat(dbRole).isEqualTo(UserRole.CUSTOMER);
        assertThat(authRole).isNotEqualTo(dbRole);
    }
}

