package com.example.demo.service;

import com.example.demo.model.dto.RegisterDTO;
import com.example.demo.model.dto.UpdateUserDTO;
import com.example.demo.model.dto.UserDTO;
import com.example.demo.model.entity.User;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public User register(RegisterDTO registerDTO) {
        // Kiểm tra số điện thoại đã tồn tại
        if (userRepository.existsByPhoneNumber(registerDTO.getPhoneNumber())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }

        // Kiểm tra email đã tồn tại
        if (registerDTO.getEmailAddress() != null && !registerDTO.getEmailAddress().isEmpty()) {
            if (userRepository.existsByEmailAddress(registerDTO.getEmailAddress())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
        }

        // Kiểm tra mật khẩu xác nhận
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        // Tạo user mới
        User user = User.builder()
                .fullName(registerDTO.getFullName())
                .emailAddress(registerDTO.getEmailAddress())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .phoneNumber(registerDTO.getPhoneNumber())
                .build();

        return userRepository.save(user);
    }

    @Override
    public boolean findExistByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    @Override
    public User findByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với số điện thoại: " + phoneNumber));
    }

    @Override
    public boolean findExistByEmail(String email) {
        return userRepository.existsByEmailAddress(email);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        return convertToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUserByRole(User.Role role) {
        List<User> users = userRepository.findByRole(role);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO updateProfile(Long userId, UpdateUserDTO dto, Long currentUserId, User.Role currentUserRole) {
        // Kiểm tra quyền: Admin/Staff có thể sửa bất kỳ, Customer chỉ sửa của mình
        if (!hasPermission(currentUserRole, "UPDATE", "USER")) {
            throw new RuntimeException("Bạn không có quyền cập nhật thông tin người dùng");
        }
        // Nếu là CUSTOMER thì phải check ownership
        if (currentUserRole == User.Role.CUSTOMER && !isOwner(currentUserId, userId)) {
            throw new RuntimeException("Bạn chỉ có thể cập nhật thông tin của chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra email trùng (nếu thay đổi)
        if (!user.getEmailAddress().equals(dto.getEmailAddress())) {
            if (userRepository.existsByEmailAddress(dto.getEmailAddress())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
        }

        // Kiểm tra phone trùng (nếu thay đổi)
        if (!user.getPhoneNumber().equals(dto.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
        }

        user.setFullName(dto.getFullName());
        user.setEmailAddress(dto.getEmailAddress());
        user.setPhoneNumber(dto.getPhoneNumber());

        User updated = userRepository.save(user);
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, Long currentUserId, User.Role currentUserRole) {
        // Kiểm tra quyền: Admin có thể xóa bất kỳ, Staff xóa customer, Customer tự xóa
        if (!hasPermission(currentUserRole, "DELETE", "USER")) {
            throw new RuntimeException("Bạn không có quyền xóa người dùng");
        }

        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra ownership nếu cần
        if (currentUserRole == User.Role.CUSTOMER) {
            if (!isOwner(currentUserId, userId)) {
                throw new RuntimeException("Bạn chỉ có thể xóa tài khoản của chính mình");
            }
        } else if (currentUserRole == User.Role.STAFF) {
            // Staff chỉ được xóa CUSTOMER
            if (userToDelete.getRole() != User.Role.CUSTOMER) {
                throw new RuntimeException("Staff chỉ có thể xóa tài khoản customer");
            }
        }
        // Admin có thể xóa bất kỳ ai

        userRepository.delete(userToDelete);
    }

    @Override
    public boolean hasPermission(User.Role userRole, String action, String resource) {
        // Quy tắc phân quyền
        switch (resource) {
            case "USER":
                switch (action) {
                    case "VIEW_ALL":
                        return userRole == User.Role.ADMIN || userRole == User.Role.STAFF;
                    case "UPDATE":
                        return true; // Tất cả có thể update (nhưng cần check ownership)
                    case "DELETE":
                        return true; // Tất cả có thể delete (nhưng cần check ownership)
                    default:
                        return false;
                }
            case "ORDER":
                switch (action) {
                    case "VIEW_ALL":
                        return userRole == User.Role.ADMIN || userRole == User.Role.STAFF;
                    case "UPDATE":
                        return userRole == User.Role.ADMIN;
                    case "DELETE":
                        return userRole == User.Role.ADMIN;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    @Override
    public boolean isOwner(Long userId, Long resourceOwnerId) {
        return userId.equals(resourceOwnerId);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Username có thể là phone number hoặc email
        return userRepository.findByPhoneNumber(username)
                .or(() -> userRepository.findByEmailAddress(username))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
    }

    private UserDTO convertToDTO(User user) {
        Long orderCount = 0L;
        try {
            if (user.getRole() == User.Role.CUSTOMER) {
                orderCount = orderRepository.countByUserId(user.getId());
            }
        } catch (Exception e) {
            // Ignore if orderRepository not available yet
            orderCount = 0L;
        }

        return UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .emailAddress(user.getEmailAddress())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .orderCount(orderCount)
                .build();
    }
}
