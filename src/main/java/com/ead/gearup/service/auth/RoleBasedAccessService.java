package com.ead.gearup.service.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.User;

import lombok.RequiredArgsConstructor;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class RoleBasedAccessService {

    private final CurrentUserService currentUserService;

    // Get the user's single role from authorities (primary source)
    public UserRole getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> UserRole.valueOf(a.getAuthority().replace("ROLE_", "")))
                .orElse(null);
    }

    // Check if the user has reqirued role
    public boolean hasRole(UserRole requiredRole) {
        UserRole currentRole = getCurrentUserRole();
        return currentRole != null && currentRole == requiredRole;
    }

    // Check if the user has any of the allowed roles
    public boolean hasAnyRole(UserRole... roles) {
        UserRole currentRole = getCurrentUserRole();
        return currentRole != null && Arrays.asList(roles).contains(currentRole);
    }

    // get user role from DB
    public UserRole getCurrentUserRoleFromDatabase() {
        try {
            User user = currentUserService.getCurrentUser();
            return user != null ? user.getRole() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
