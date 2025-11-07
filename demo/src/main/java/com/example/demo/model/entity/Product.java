package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="producName", length = 100)
    private String name;

    @Column(unique = true)
    private String sku;

    private String type;

    private String brand;

    private String model;

    @Column(name = "description")
    private String description;

    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity; // Số lượng còn trong kho

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @OneToMany (mappedBy = "product",
            cascade = CascadeType.ALL,     // cascade sang con
            orphanRemoval = true,          // xoá con “mồ côi”
            fetch = FetchType.LAZY)
    private List<ProductImage> productImages;
}
