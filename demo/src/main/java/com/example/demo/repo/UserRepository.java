package com.example.demo.repo;

import com.example.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // Có thể thêm các method custom sau này nếu cần filter
}