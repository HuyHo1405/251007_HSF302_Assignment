package com.example.demo.service;

import com.example.demo.model.dto.OrderDTO;
import java.util.List;

public interface OrderService {
    OrderDTO createOrder(OrderDTO dto);
    OrderDTO getOrderById(Long id);
    List<OrderDTO> findByUser(Long userId);
    List<OrderDTO> findByStatus(String status);
    List<OrderDTO> getAllOrders();
    void updateStatus(Long id, String status);
    void cancelOrder(Long id); // user cancel
    void deleteOrder(Long id); // staff/admin delete
}

