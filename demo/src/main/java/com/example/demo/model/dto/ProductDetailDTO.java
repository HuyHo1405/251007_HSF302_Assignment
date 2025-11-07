package com.example.demo.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProductDetailDTO {
    private Long id;

    private String name;

    private String sku;

    private String type;

    private String brand;

    private String model;

    private String description;

    private Double unitPrice;

    private Integer warrantyMonths;

    private String status;

    private LocalDateTime createdAt;

    private String imageUrl;
}
