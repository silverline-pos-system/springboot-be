package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.UserDTO;
import com.silverline.erp.module.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/admin/users/count
     * Returns total number of users.
     */
    @GetMapping("/count")
    public Long getAllUserCount() {
        return userService.getAllUserCount();
    }

    /**
     * GET /api/v1/admin/users
     * Get all users
     */
    @GetMapping("")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/v1/admin/users/search
     * Search users
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam("q") String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    /**
     * POST /api/v1/admin/users/register-manager
     * Register a new manager
     */
    @PostMapping("/register-manager")
    public ResponseEntity<UserDTO> registerManager(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.registerManager(userDTO));
    }

    /**
     * PUT /api/v1/admin/users/{userId}
     * Update user details
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long userId, @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(userId, userDTO));
    }

    /**
     * DELETE /api/v1/admin/users/{userId}
     * Delete a user
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/toggle-status
     * Toggle user status
     */
    @PatchMapping("/{userId}/toggle-status")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/v1/admin/users/managers
     * Get list of managers
     */
    @GetMapping("/managers")
    public ResponseEntity<List<UserDTO>> getManagers() {
        return ResponseEntity.ok(userService.getManagers());
    }
}

