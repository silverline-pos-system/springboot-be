package com.silverline.erp.module.admin.service;

import com.silverline.erp.module.admin.dto.UserDTO;

import java.util.List;

public interface UserService {
    List<UserDTO> getAllUsers();

    List<UserDTO> searchUsers(String query);

    UserDTO registerManager(UserDTO userDTO);

    UserDTO updateUser(Long userId, UserDTO userDTO);

    void deleteUser(Long userId);

    void toggleUserStatus(Long userId);

    List<UserDTO> getManagers();

    /**
     * Return total number of user profiles.
     */
    Long getAllUserCount();
}

