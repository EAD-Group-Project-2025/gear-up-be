package com.ead.gearup.service.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.AccessDeniedException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrinciple userDetails) {
            return userDetails.getUserId();
        }

        throw new AccessDeniedException("Unauthorized");
    }

    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Get the current user's role
     */
    public UserRole getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    /*
     * Get the current user's role-specific entity ID
     * The entity ID (volunteerId, sponsorId, organizationId, or userId for admin),
     * or null if not found
     */
    // public Long getCurrentEntityId() {
    // Long userId = getCurrentUserId();
    // User user = userRepository.findById(userId)
    // .orElseThrow(() -> new UserNotFoundException("User not found"));

    // if (user.getRole() == null) {
    // return null;
    // }

    // return switch (user.getRole()) {
    // case CUSTOMER -> volunteerRepository.findByUser(user)
    // .map(volunteer -> volunteer.getVolunteerId())
    // .orElse(null);
    // case EMPLOYEE -> sponsorRepository.findByUser(user)
    // .map(sponsor -> sponsor.getSponsorId())
    // .orElse(null);
    // case ADMIN -> user.getUserId();
    // case PUBLIC -> null;
    // };
    // }
}
