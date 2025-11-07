package com.example.demo.model.mapper;



import com.example.demo.model.dto.ProductDetailDTO;
import com.example.demo.model.dto.ProductListDTO;
import com.example.demo.model.entity.Product;

import java.util.Optional;

public class ProductMapper {
    //thay vì trả về Product entity, ta trả về ProductListDTO
    //static gọi thẳng ProductMapper.toDTO(p), không cần tạo object hay inject bean
    public static ProductListDTO toListDTO(Product pro){
        String firstUrl = Optional.ofNullable(pro.getProductImages())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0).getUrl())
                .orElse(null);                   // có thể để null hoặc placeholder

        return ProductListDTO.builder()
                .id(pro.getId())
                .name(pro.getName())
                .brand(pro.getBrand())
                .unitPrice(pro.getUnitPrice())
                .imageUrl(firstUrl)
                .build();
    }


    public static ProductDetailDTO toDetailDTO(Product pro){
        return ProductDetailDTO.builder()
                .id(pro.getId())
                .name(pro.getName())
                .sku(pro.getSku())
                .type(pro.getType())
                .brand(pro.getBrand())
                .model(pro.getModel())
                .description(pro.getDescription())
                .unitPrice(pro.getUnitPrice())
                .warrantyMonths(pro.getWarrantyMonths())
                .status(pro.getStatus())
                .createdAt(pro.getCreatedAt())
                .imageUrl(pro.getProductImages().get(0).getUrl())
                .build();
    }
 }


