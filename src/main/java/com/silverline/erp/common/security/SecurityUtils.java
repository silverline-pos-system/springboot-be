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
     *
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
     *
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
     *
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
     *
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

    /**
     * Generates a cryptographically secure temporary password of 12 characters.
     */
    public static String generateSecureTemporaryPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specials = "!@#$%^&*";
        String allChars = upper + lower + digits + specials;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(12);

        // Ensure at least one character from each group is present
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(specials.charAt(random.nextInt(specials.length())));

        // Fill the rest with random characters
        for (int i = 4; i < 12; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the characters
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    /**
     * Hashes a token string using SHA-256.
     */
    public static String hashToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
