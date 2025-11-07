package com.example.demo.repo;

import com.example.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByEmailAddress(String emailAddress);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmailAddress(String emailAddress);
    List<User> findByRole(User.Role role);
}