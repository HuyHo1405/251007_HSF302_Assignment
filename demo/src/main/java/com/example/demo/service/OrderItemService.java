package com.example.demo.service;

import com.example.demo.model.dto.OrderItemDTO;

import java.util.List;

public interface OrderItemService {
    // Thêm sản phẩm vào đơn
    OrderItemDTO addOrderItem(OrderItemDTO dto);

    // Lấy danh sách item của 1 đơn
    List<OrderItemDTO> getItemsByOrder(Long orderId);

    // Lấy chi tiết 1 dòng sản phẩm
    OrderItemDTO getItemById(Long id);

    // Cập nhật thông tin dòng sản phẩm trong đơn
    OrderItemDTO updateOrderItem(Long id, OrderItemDTO dto);

    // Xóa dòng sản phẩm khỏi đơn
    void deleteOrderItem(Long id);

    // (tuỳ chọn) Tính lại tổng tiền cho đơn khi thêm/sửa/xoá item
    void recalculateOrderTotal(Long orderId);
}
