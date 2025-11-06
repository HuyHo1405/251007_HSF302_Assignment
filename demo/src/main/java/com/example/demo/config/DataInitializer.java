package com.example.demo.config;

import com.example.demo.model.entity.*;
import com.example.demo.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public void run(String... args) {
<<<<<<< Updated upstream
=======
        if(userRepository.count()!=0){
            return;
        }

>>>>>>> Stashed changes
        // USERS
        User user1 = new User();
        user1.setFullName("Nguyen Van A");
        user1.setEmailAddress("a@test.com");
        user1.setPhoneNumber("0123456789");
<<<<<<< Updated upstream
=======
        user1.setPassword("$2a$10$1NjnEoPQbAh7nEQIkbwjquKpIbLL3.A9U1WLeogFy9qW3rlo1R29q");
>>>>>>> Stashed changes
        user1.setCreatedAt(LocalDateTime.now());
        userRepository.save(user1);

        User user2 = new User();
        user2.setFullName("Tran Thi B");
        user2.setEmailAddress("b@test.com");
        user2.setPhoneNumber("0987654321");
<<<<<<< Updated upstream
=======
        user2.setPassword("$2a$10$1NjnEoPQbAh7nEQIkbwjquKpIbLL3.A9U1WLeogFy9qW3rlo1R29q");
>>>>>>> Stashed changes
        user2.setCreatedAt(LocalDateTime.now());
        userRepository.save(user2);

        // PRODUCTS
        Product prod1 = new Product();
        prod1.setName("CPU Intel i5");
        prod1.setSku("CPU001");
        prod1.setType("cpu");
        prod1.setBrand("Intel");
        prod1.setModel("i5-12400F");
        prod1.setDescription("CPU thế hệ 12");
        prod1.setUnitPrice(4000000d);
        prod1.setWarrantyMonths(24);
        prod1.setStatus("active");
        prod1.setCreatedAt(LocalDateTime.now());
        productRepository.save(prod1);

        Product prod2 = new Product();
        prod2.setName("RAM Kingston 8GB");
        prod2.setSku("RAM001");
        prod2.setType("ram");
        prod2.setBrand("Kingston");
        prod2.setModel("KVR8GB");
        prod2.setDescription("Ram DDR4 8GB");
        prod2.setUnitPrice(950000d);
        prod2.setWarrantyMonths(12);
        prod2.setStatus("active");
        prod2.setCreatedAt(LocalDateTime.now());
        productRepository.save(prod2);

        // ORDERS
        Order order1 = new Order();
        order1.setUser(user1);
        order1.setStatus("pending");
        order1.setShippingAddress("123 Đường ABC, Quận 1, TP.HCM");
        order1.setCreatedAt(LocalDateTime.now());
        order1.setUpdatedAt(LocalDateTime.now());

        // ORDER ITEMS
        OrderItem item1 = new OrderItem();
        item1.setOrder(order1);
        item1.setProduct(prod1);
        item1.setQuantity(1);
        item1.setUnitPrice(prod1.getUnitPrice());

        OrderItem item2 = new OrderItem();
        item2.setOrder(order1);
        item2.setProduct(prod2);
        item2.setQuantity(2);
        item2.setUnitPrice(prod2.getUnitPrice());

        order1.setOrderItems(Arrays.asList(item1, item2));
        order1.setTotalPrice(
                item1.getUnitPrice() * item1.getQuantity() +
                        item2.getUnitPrice() * item2.getQuantity()
        );

        orderRepository.save(order1); // cascade sẽ save cả items
    }
}