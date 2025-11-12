package com.example.demo.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BestSellerProductDTO {
    private Long id;
    private String name;
    private String brand;
    private Double unitPrice;
    private Long totalSold;
    private String imageUrl;
}




