package com.example.demo.model.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private String orderCode;
    private String paymentMethod; // cash, card, banking, momo, vnpay
    private Double amount;
    private String status; // pending, completed, failed
    private String responseCode;
    private LocalDateTime paidAt;
}
