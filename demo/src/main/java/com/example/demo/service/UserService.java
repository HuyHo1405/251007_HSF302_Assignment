package com.example.demo.service;

import com.example.demo.model.dto.RegisterDTO;
import com.example.demo.model.dto.UpdateUserDTO;
import com.example.demo.model.dto.UserDTO;
import com.example.demo.model.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;


import java.util.List;

public interface UserService extends UserDetailsService {

    User register(RegisterDTO registerDTO);

    boolean findExistByPhoneNumber(String phoneNumber);

    User findByEmail(String email);

    User findByPhone(String phoneNumber);

    boolean findExistByEmail(String email);

    UserDTO getUserById(Long id);

    List<UserDTO> getAllUsers();

    List<UserDTO> getUserByRole(User.Role role);

    UserDTO updateProfile(Long userId, UpdateUserDTO dto, Long currentUserId, User.Role currentUserRole);

    void deleteUser(Long userId, Long currentUserId, User.Role currentUserRole);

    boolean hasPermission(User.Role userRole, String action, String resource);

    boolean isOwner(Long userId, Long resourceOwnerId);

}
