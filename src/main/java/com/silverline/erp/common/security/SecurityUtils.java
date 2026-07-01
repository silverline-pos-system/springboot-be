package com.silverline.erp.common.security;

import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.UserProfile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reusable security utility methods.
 * Eliminates duplicate getCurrentUserId() implementations across controllers and services.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class - no instantiation
    }

    /**
     * Get the currently authenticated user's ID.
     * @return userId or null if not authenticated
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getUserId();
        }
        return null;
    }

    /**
     * Get the currently authenticated user's username.
     * @return username or null if not authenticated
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getUsername();
        }
        return null;
    }

    /**
     * Get the currently authenticated user's role.
     * @return role or null if not authenticated
     */
    public static Role getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getRole();
        }
        return null;
    }

    /**
     * Get the full UserProfile of the currently authenticated user.
     * @return UserProfile or null if not authenticated
     */
    public static UserProfile getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return (UserProfile) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Check if the current user has a specific role.
     */
    public static boolean hasRole(Role role) {
        Role currentRole = getCurrentUserRole();
        return currentRole != null && currentRole == role;
    }

    /**
     * Check if the current user is a manager or above.
     */
    public static boolean isManagerOrAbove() {
        Role role = getCurrentUserRole();
        if (role == null) return false;
        return role == Role.MANAGER || role == Role.SUPER_ADMIN;
    }

    /**
     * Check if current user is authenticated.
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserProfile;
    }
}
