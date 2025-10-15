package com.example.demo.service;

import com.example.demo.model.dto.PaymentDTO;
import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.Payment;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;

    @Override
    @Transactional
    public PaymentDTO createPayment(PaymentDTO dto) {
        // Kiểm tra order có tồn tại không
        Order order = orderRepo.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Tạo payment mới
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(dto.getPaymentMethod())
                .amount(dto.getAmount())
                .status("pending")
                .build();

        Payment saved = paymentRepo.save(payment);
        return toDTO(saved);
    }

    @Override
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return toDTO(payment);
    }

    @Override
    public List<PaymentDTO> getPaymentsByOrderId(Long orderId) {
        return paymentRepo.findByOrderId(orderId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentDTO completePayment(Long id) {
        Payment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus("completed");
        payment.setPaidAt(LocalDateTime.now());

        // Cập nhật order status thành paid nếu cần
        Order order = payment.getOrder();
        if ("pending".equals(order.getStatus())) {
            order.setStatus("paid");
            orderRepo.save(order);
        }

        Payment saved = paymentRepo.save(payment);
        return toDTO(saved);
    }

    // Helper method: Entity -> DTO
    private PaymentDTO toDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
