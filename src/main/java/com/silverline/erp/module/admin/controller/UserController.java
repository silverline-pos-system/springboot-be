package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.UserDTO;
import com.silverline.erp.module.admin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Profile Operations", description = "APIs for administrators to view, modify, register, disable, and manage user accounts in the ERP system")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get total user count", description = "Returns the total number of users registered in the system")
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    @GetMapping("/count")
    public Long getAllUserCount() {
        return userService.getAllUserCount();
    }

    @Operation(summary = "Get all users list", description = "Retrieves a comprehensive list of all registered users in the system")
    @ApiResponse(responseCode = "200", description = "Users list retrieved successfully")
    @GetMapping("")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Search users by query", description = "Finds users matching a search term query (matches username, email, full name, or employee ID)")
    @ApiResponse(responseCode = "200", description = "Users search completed successfully")
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam("q") String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @Operation(summary = "Register a new manager profile", description = "Registers a new manager account in the system directly in ACTIVE status")
    @ApiResponse(responseCode = "200", description = "Manager registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload or duplicate credentials")
    @PostMapping("/register-manager")
    public ResponseEntity<UserDTO> registerManager(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.registerManager(userDTO));
    }

    @Operation(summary = "Update user details", description = "Modifies user details (full name, email, phone, role) for a specific user ID")
    @ApiResponse(responseCode = "200", description = "User details updated successfully")
    @ApiResponse(responseCode = "404", description = "User profile not found")
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long userId, @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(userId, userDTO));
    }

    @Operation(summary = "Delete user account", description = "Deletes a user account from the system by user ID")
    @ApiResponse(responseCode = "24", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User profile not found")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle user active status", description = "Toggles user status between active and suspended states")
    @ApiResponse(responseCode = "200", description = "User status toggled successfully")
    @ApiResponse(responseCode = "404", description = "User profile not found")
    @PatchMapping("/{userId}/toggle-status")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get list of all managers", description = "Retrieves profiles for all users who hold the MANAGER role")
    @ApiResponse(responseCode = "200", description = "Managers list retrieved successfully")
    @GetMapping("/managers")
    public ResponseEntity<List<UserDTO>> getManagers() {
        return ResponseEntity.ok(userService.getManagers());
    }
}
