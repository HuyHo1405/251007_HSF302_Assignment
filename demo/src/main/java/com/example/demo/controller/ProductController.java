package com.example.demo.controller;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

@Controller
public class ProductController {
    public String listAllProducts() {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        return "product/list";
    }
}
