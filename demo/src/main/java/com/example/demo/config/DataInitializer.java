package com.example.demo.config;

import com.example.demo.model.entity.*;
import com.example.demo.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProductImageRepo productImageRepo;

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
        products.add(createProduct("Intel Core i9-14900K", "CPU Intel thế hệ 14, 24 nhân 32 luồng, xung nhịp tối đa 6.0GHz", 14990000.0, 25,"Linh kiện",12));
        products.add(createProduct("AMD Ryzen 9 7950X", "CPU AMD Zen 4, 16 nhân 32 luồng, xung nhịp tối đa 5.7GHz", 13990000.0, 30, "Linh kiện",24));
        products.add(createProduct("NVIDIA RTX 4090 24GB", "VGA cao cấp nhất, 24GB GDDR6X, Ray Tracing thế hệ mới", 49990000.0, 15, "Linh kiện",12));
        products.add(createProduct("ASUS ROG Strix RTX 4070 Ti", "VGA gaming cao cấp, 12GB GDDR6X, RGB Aura Sync", 24990000.0, 35, "Linh kiện",24));
        products.add(createProduct("Corsair Vengeance 64GB DDR5", "RAM DDR5-6000MHz, Kit 2x32GB, RGB LED", 7990000.0, 50, "Linh kiện",24));
        products.add(createProduct("Samsung 990 PRO 2TB NVMe", "SSD M.2 NVMe Gen 4, tốc độ đọc 7450MB/s", 5990000.0, 60, "Linh kiện",24));
        products.add(createProduct("ASUS ROG Maximus Z790 Hero", "Bo mạch chủ Z790, Socket LGA1700, WiFi 7, DDR5", 12990000.0, 20, "Linh kiện",12));
        products.add(createProduct("Corsair HX1500i 1500W", "Nguồn Platinum 1500W, Full Modular, RGB", 8990000.0, 25, "Linh kiện",36));
        products.add(createProduct("Lian Li O11 Dynamic EVO", "Case ATX cao cấp, kính cường lực, hỗ trợ tản nhiệt nước", 4990000.0, 40, "Linh kiện",36));
        products.add(createProduct("NZXT Kraken Z73 RGB", "Tản nhiệt nước AIO 360mm, màn hình LCD 2.36 inch", 6990000.0, 35, "Linh kiện",36));
        products.add(createProduct("Dell XPS 13 9340", "Ultrabook 13\", Intel Core Ultra, màn 13.4\" FHD+, nhẹ ~1.2kg", 25990000.0, 25,"Laptop",36));
        products.add(createProduct("Apple MacBook Air 13\" M2", "M2, 8-core CPU, 8-core GPU, pin lâu, màn Liquid Retina 13.6", 27990000.0, 30,"Laptop",24));
        products.add(createProduct("ASUS ROG Zephyrus G14 (2024)", "RTX 4060, Ryzen 7, 14\" 165Hz, máy gaming mỏng nhẹ", 44990000.0, 15,"Laptop",48));
        products.add(createProduct("HP Spectre x360 14", "2-in-1 OLED 13.5\", Intel Core i7, cảm ứng, bút", 32990000.0, 20,"Laptop",36));
        products.add(createProduct("Lenovo Legion 5 Pro 16", "16\" 165Hz, RTX 4070, Ryzen 7, tản nhiệt tốt, bàn phím full-size", 37990000.0, 18,"Laptop",36));
        products.add(createProduct("Acer Swift Go 14 OLED", "14\" OLED 2.8K, Intel Core i5, mỏng nhẹ ~1.3kg, Wi-Fi 6E", 21990000.0, 22,"Laptop",24));

        //Tao imageUrl
        List<Product> prod = productRepository.findAll(Sort.by("id").ascending());
// Nhớ đảm bảo có đủ số product trước khi gọi get(i)
        createProductImage("https://product.hstatic.net/200000722513/product/n36733-001-arl-i9k-univ_b4cd53ec34294ed9bea8be6f28991d91_master.png", products.get(0));
        createProductImage("https://product.hstatic.net/200000722513/product/gearvn-bo-vi-xu-ly-amd-ryzen-7-8700g-1_8dc602aee46e43a89d055cce370bf51f_master.png",      products.get(1));
        createProductImage("https://product.hstatic.net/200000722513/product/-man-hinh-asus-dual-geforce-rtx-3050-oc-edition-6gb-dual-rtx3050-o6g-5_32785b8f85e2429a84fcc27a80f82c1b_master.png",       products.get(2));
        createProductImage("https://product.hstatic.net/200000722513/product/fwebp__10__1d22cf39c094494bb772b5bb1c002172_master.png",    products.get(3));
        createProductImage("https://product.hstatic.net/200000722513/product/m_corsair_vengeance_rgb_128gb__2x64gb__5600_ddr5__cmh128gx5m2b6400c42__1a6b85d9ff7c48f08ec540b44530f7d6_master.png", products.get(4));
        createProductImage("https://product.hstatic.net/200000722513/product/-am_001_front_black-gallery-1600x1200_d5430da92de74a7c9d7b35a7ae9b3587_b2e724a266834268bead0b9ab068d99c_master.png", products.get(5));
        createProductImage("https://product.hstatic.net/200000722513/product/rog-maximus-z790-formula-01_0833e143741c4a44a92799d0375eb655_master.jpg",  products.get(6));
        createProductImage("https://product.hstatic.net/200000722513/product/gearvn-nguon-corsair-hx1500i-full-modular-80-plus-platinum-5_941b6db02f2a4848ac2503a49d00472f_master.png",products.get(7));
        createProductImage("https://product.hstatic.net/200000722513/product/o11dergb-001_6d0a8573f7174ff1b1c9443d5e293150_master.png",products.get(8));
        createProductImage("https://product.hstatic.net/200000722513/product/z53-rgb-black__1__f729c25973d04f51a0b221b1e3169f30_6965423765c04392ab3fb694b3b708d9_master.jpeg",    products.get(9));
        createProductImage("https://cdn.hstatic.net/products/200000722513/ava_0a30ce7dc91d4bf9bc058f46086f636d_master.png",    products.get(10));
        createProductImage("https://product.hstatic.net/200000722513/product/cbook-air-m2-10gpu-8gb-512gb-silver-2_9996b049f85648169fad6e9f71ed1bc1_6c61a5a4d2a44212af3826b301a4af43_master.jpg",    products.get(11));
        createProductImage("https://product.hstatic.net/200000722513/product/asus-rog-zephyrus-g14-ga403uv-qs171w_4aef4676f5ef4fddbb5fd6d0c5fec7d5_295d6371914e47bf83d840031a95751f_medium.png",    products.get(12));
        createProductImage("https://product.hstatic.net/200000722513/product/6l1y2pa_bc802bb991424d33a1104ae3c7cf155c_master.png",    products.get(13));createProductImage("https://product.hstatic.net/200000722513/product/legion_pro_5_16iax10_ct1_03_4afc5e6da4154d6aab7cde5284959933_master.png",    products.get(14));
        createProductImage("https://product.hstatic.net/200000722513/product/wift-go-ai-2024-gen-2-sfg14-73-71zx_1_ccc2cc55cf11451086e09eac92cae064_cebd993058e6471d8b7b7d2dccb51ca3_master.png",    products.get(15));

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

    private Product createProduct(String name, String description, Double price, Integer stockQuantity, String type, Integer warrantyMonths) {
        Product product = Product.builder()
                .name(name)
                .description(description)
                .unitPrice(price)
                .stockQuantity(stockQuantity)
                .createdAt(LocalDateTime.now())
                .type(type)
                .warrantyMonths(warrantyMonths)
                .build();
        return productRepository.save(product);
    }

    @Transactional
    public ProductImage createProductImage(String imageUrl, Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl must not be blank");
        }

        // Khởi tạo list nếu null
        if (product.getProductImages() == null) {
            product.setProductImages(new ArrayList<>());
        }

        // Không thêm trùng URL
        boolean exists = product.getProductImages().stream()
                .anyMatch(pi -> imageUrl.equalsIgnoreCase(pi.getUrl()));
        if (exists) {
            // đã có -> trả lại ảnh cũ (hoặc return null tuỳ ý)
            return product.getProductImages().stream()
                    .filter(pi -> imageUrl.equalsIgnoreCase(pi.getUrl()))
                    .findFirst()
                    .orElse(null);
        }

        ProductImage proImage = ProductImage.builder()
                .url(imageUrl)
                .product(product)      // phía sở hữu
                .build();

        product.getProductImages().add(proImage); // gắn ngược lại để đồng bộ bộ nhớ
        return productImageRepo.save(proImage);   // lưu ảnh (không cần save product)
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
