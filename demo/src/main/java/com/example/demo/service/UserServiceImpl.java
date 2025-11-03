package com.example.demo.service;

import com.example.demo.model.dto.RegisterDTO;
import com.example.demo.model.entity.User;
import com.example.demo.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Username có thể là phone number hoặc email
        return userRepository.findByPhoneNumber(username)
                .or(() -> userRepository.findByEmailAddress(username))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
    }
}
