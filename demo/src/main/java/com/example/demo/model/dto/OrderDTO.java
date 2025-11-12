package com.example.demo.model.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private Long userId;
    private String userFullName;
    private String status;
    private String userPhoneNumber;
    private String userEmail;
    private String paymentStatus;
    private Double totalPrice;
    private String shippingAddress;
    private String createdAt;
    private String updatedAt;
    @Builder.Default
    private List<OrderItemDTO> items = new ArrayList<>();
}
