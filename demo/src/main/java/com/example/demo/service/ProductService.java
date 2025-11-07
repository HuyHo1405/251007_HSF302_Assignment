package com.example.demo.service;

import com.example.demo.model.dto.ProductDTO;
import com.example.demo.model.entity.Product;

import java.util.List;

public interface ProductService {
    public List<Product> getAllProducts();
}
