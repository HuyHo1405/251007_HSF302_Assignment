package com.example.demo.service;

import com.example.demo.model.dto.PaymentDTO;
import java.util.List;

public interface PaymentService {

    PaymentDTO createPayment(PaymentDTO dto);

    PaymentDTO getPaymentById(Long id);

    List<PaymentDTO> getPaymentsByOrderId(Long orderId);

    PaymentDTO completePayment(Long id);
}
