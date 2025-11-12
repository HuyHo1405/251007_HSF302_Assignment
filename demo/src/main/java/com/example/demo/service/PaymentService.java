package com.example.demo.service;

import com.example.demo.model.entity.Payment;
import com.example.demo.model.dto.PaymentDTO;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    // VNPay methods
    String createVnpayPayment(Long orderId); // Tạo VNPay URL và redirect
    Map<String, String> handleVnpayReturn(Map<String, String> vnpayParams); // Xử lý callback từ VNPay

    // VNPay Refund
    Map<String, String> refundVnpayPayment(Long orderId, String reason); // Hoàn tiền qua VNPay API

    // COD method
    Payment createCODPayment(Long orderId); // Tạo thanh toán COD

    // Get methods
    Payment getPaymentByOrderCode(String orderCode); // Tìm payment theo orderCode
    List<Payment> getPaymentsByOrderId(Long orderId); // Danh sách payments của 1 order
    List<PaymentDTO> getPaymentDTOsByOrderId(Long orderId); // Thêm method trả về DTO

    // Mock/Test method
    Map<String, String> simulateVnpaySuccess(String orderCode); // Giả lập thanh toán thành công
}
