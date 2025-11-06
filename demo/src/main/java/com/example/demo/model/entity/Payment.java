package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "payment_method")
    private String paymentMethod; // cash, card, banking, momo, vnpay

    private Double amount;

    private String status; // pending, completed, failed

    @Column(name = "order_code", unique = true, length = 50)
    private String orderCode;

    @Column(name = "transactkoion_ref", length = 50)
    private String transactionRef;

    @Column(name = "response_code", length = 10)
    private String responseCode; // Mã response VNPay (00 = success)

    @Column(name = "raw_response_data", columnDefinition = "NVARCHAR(MAX)")
    private String rawResponseData; // Lưu toàn bộ response VNPay để debug

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}