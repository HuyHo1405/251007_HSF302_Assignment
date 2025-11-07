package com.example.demo.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
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
}
