package com.example.demo.repo;

import com.example.demo.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Có thể thêm các method custom sau này nếu cần filter
}