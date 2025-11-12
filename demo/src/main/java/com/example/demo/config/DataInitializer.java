package com.example.demo.config;

import com.example.demo.model.entity.*;
import com.example.demo.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Chỉ chạy nếu database còn trống
        if (userRepository.count() > 0) {
            System.out.println("Database already initialized. Skipping data initialization.");
            return;
        }

        System.out.println("Initializing database with sample data...");

        // 1. Tạo Users
        User admin = createUser("Admin User", "0123456789", "admin@shop.com", "123456", User.Role.ADMIN);
        User staff = createUser("Staff User", "0987654321", "staff@shop.com", "123456", User.Role.STAFF);
        User customer1 = createUser("Nguyễn Văn A", "0912345678", "customer1@gmail.com", "123456", User.Role.CUSTOMER);
        User customer2 = createUser("Trần Thị B", "0923456789", "customer2@gmail.com", "123456", User.Role.CUSTOMER);
        User customer3 = createUser("Lê Văn C", "0934567890", "customer3@gmail.com", "123456", User.Role.CUSTOMER);

        // 2. Tạo 10 Products - Computer Components for PC Shop
        List<Product> products = new ArrayList<>();
        products.add(createProduct("Intel Core i9-14900K", "CPU Intel thế hệ 14, 24 nhân 32 luồng, xung nhịp tối đa 6.0GHz", 14990000.0, 25));
        products.add(createProduct("AMD Ryzen 9 7950X", "CPU AMD Zen 4, 16 nhân 32 luồng, xung nhịp tối đa 5.7GHz", 13990000.0, 30));
        products.add(createProduct("NVIDIA RTX 4090 24GB", "VGA cao cấp nhất, 24GB GDDR6X, Ray Tracing thế hệ mới", 49990000.0, 15));
        products.add(createProduct("ASUS ROG Strix RTX 4070 Ti", "VGA gaming cao cấp, 12GB GDDR6X, RGB Aura Sync", 24990000.0, 35));
        products.add(createProduct("Corsair Vengeance 64GB DDR5", "RAM DDR5-6000MHz, Kit 2x32GB, RGB LED", 7990000.0, 50));
        products.add(createProduct("Samsung 990 PRO 2TB NVMe", "SSD M.2 NVMe Gen 4, tốc độ đọc 7450MB/s", 5990000.0, 60));
        products.add(createProduct("ASUS ROG Maximus Z790 Hero", "Bo mạch chủ Z790, Socket LGA1700, WiFi 7, DDR5", 12990000.0, 20));
        products.add(createProduct("Corsair HX1500i 1500W", "Nguồn Platinum 1500W, Full Modular, RGB", 8990000.0, 25));
        products.add(createProduct("Lian Li O11 Dynamic EVO", "Case ATX cao cấp, kính cường lực, hỗ trợ tản nhiệt nước", 4990000.0, 40));
        products.add(createProduct("NZXT Kraken Z73 RGB", "Tản nhiệt nước AIO 360mm, màn hình LCD 2.36 inch", 6990000.0, 35));

        // 3. Tạo Orders - 3 đơn cho mỗi status
        // Status: pending (3 đơn)
        createOrderWithItems(customer1, "123 Nguyễn Huệ, Q1, TP.HCM", "confirmed", products, 0, 1, 2);
        createOrderWithItems(customer2, "456 Lê Lợi, Q1, TP.HCM", "confirmed", products, 3, 4, 5);
        createOrderWithItems(customer3, "789 Trần Hưng Đạo, Q5, TP.HCM", "confirmed", products, 6, 7, 8);

        // Status: paid (3 đơn)
        createOrderWithItems(customer1, "123 Nguyễn Huệ, Q1, TP.HCM", "paid", products, 1, 2, 3);
        createOrderWithItems(customer2, "456 Lê Lợi, Q1, TP.HCM", "paid", products, 4, 5, 6);
        createOrderWithItems(customer3, "789 Trần Hưng Đạo, Q5, TP.HCM", "paid", products, 7, 8, 9);

        // Status: shipping (3 đơn)
        createOrderWithItems(customer1, "123 Nguyễn Huệ, Q1, TP.HCM", "shipping", products, 2, 3, 4);
        createOrderWithItems(customer2, "456 Lê Lợi, Q1, TP.HCM", "shipping", products, 5, 6, 7);
        createOrderWithItems(customer3, "789 Trần Hưng Đạo, Q5, TP.HCM", "shipping", products, 8, 9, 0);

        // Status: delivered (3 đơn)
        createOrderWithItems(customer1, "123 Nguyễn Huệ, Q1, TP.HCM", "delivered", products, 3, 4, 5);
        createOrderWithItems(customer2, "456 Lê Lợi, Q1, TP.HCM", "delivered", products, 6, 7, 8);
        createOrderWithItems(customer3, "789 Trần Hưng Đạo, Q5, TP.HCM", "delivered", products, 9, 0, 1);

        // Status: cancelled (3 đơn)
        createOrderWithItems(customer1, "123 Nguyễn Huệ, Q1, TP.HCM", "cancelled", products, 4, 5, 6);
        createOrderWithItems(customer2, "456 Lê Lợi, Q1, TP.HCM", "cancelled", products, 7, 8, 9);
        createOrderWithItems(customer3, "789 Trần Hưng Đạo, Q5, TP.HCM", "cancelled", products, 0, 1, 2);

        System.out.println("Database initialization completed!");
        System.out.println("=== TEST ACCOUNTS ===");
        System.out.println("Admin: 0123456789 / 123456");
        System.out.println("Staff: 0987654321 / 123456");
        System.out.println("Customer: 0912345678 / 123456");
        System.out.println("====================");
    }

    private User createUser(String fullName, String phoneNumber, String email, String password, User.Role role) {
        User user = User.builder()
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .emailAddress(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private Product createProduct(String name, String description, Double price, Integer stockQuantity) {
        Product product = Product.builder()
                .name(name)
                .description(description)
                .unitPrice(price)
                .stockQuantity(stockQuantity)
                .createdAt(LocalDateTime.now())
                .build();
        return productRepository.save(product);
    }

    private void createOrderWithItems(User user, String address, String status, List<Product> products, int... productIndexes) {
        // Tạo Order
        Order order = Order.builder()
                .user(user)
                .shippingAddress(address)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        // Tính tổng tiền và tạo OrderItems
        double totalPrice = 0.0;
        List<OrderItem> items = new ArrayList<>();

        for (int index : productIndexes) {
            Product product = products.get(index);
            int quantity = (int) (Math.random() * 3) + 1; // Random 1-3
            double subtotal = product.getUnitPrice() * quantity;
            totalPrice += subtotal;

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getUnitPrice())
                    .build();
            items.add(item);
        }

        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);

        // Lưu OrderItems
        for (OrderItem item : items) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }
    }
}
