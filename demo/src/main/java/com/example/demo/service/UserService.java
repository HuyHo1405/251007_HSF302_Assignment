package com.example.demo.service;

import com.example.demo.model.dto.RegisterDTO;
import com.example.demo.model.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {

    User register(RegisterDTO registerDTO);

    boolean findExistByPhoneNumber(String phoneNumber);

    User findByEmail(String email);

    User findByPhone(String phoneNumber);

    boolean findExistByEmail(String email);
}
