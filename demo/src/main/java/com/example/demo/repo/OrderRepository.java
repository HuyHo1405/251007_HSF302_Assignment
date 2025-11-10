package com.example.demo.repo;

import com.example.demo.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(String status);

    Long countByUserId(Long userId);

    @Query("SELECT p.name as productName, " +
            "SUM(oi.quantity) as totalQuantity, " +
            "SUM(oi.quantity * oi.unitPrice) as totalRevenue " +
            "FROM OrderItem oi " +
            "JOIN oi.product p " +
            "GROUP BY p.id, p.name " +
            "ORDER BY totalQuantity DESC")
    List<Map<String, Object>> findTop10Products();
}