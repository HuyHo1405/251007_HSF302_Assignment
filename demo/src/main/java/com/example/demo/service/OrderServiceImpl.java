package com.example.demo.service;

import com.example.demo.model.dto.OrderDTO;
import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.OrderItem;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.User;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.OrderItemRepository;
import com.example.demo.repo.ProductRepository;
import com.example.demo.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final ProductService productService; // Thêm ProductService

    // Helper convert
    private OrderDTO toDTO(Order order) {
        List<OrderItemDTO> items = order.getOrderItems().stream().map(item -> {
            Product p = item.getProduct();
            return OrderItemDTO.builder()
                    .id(item.getId())
                    .productId(p.getId())
                    .productName(p.getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getUnitPrice() * item.getQuantity())
                    .build();
        }).collect(Collectors.toList());
        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userFullName(order.getUser() != null ? order.getUser().getFullName() : null)
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt() + "")
                .updatedAt(order.getUpdatedAt() + "")
                .items(items)
                .build();
    }

    @Override
    public OrderDTO createOrder(OrderDTO dto) {
        Order order = new Order();
        // 1. BẮT BUỘC phải set user TỪ userId
        if (dto.getUserId() == null) throw new IllegalArgumentException("UserId không được null");
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy userId: " + dto.getUserId()));
        order.setUser(user);

        // 2. Các field khác
        order.setStatus("pending");
        order.setShippingAddress(dto.getShippingAddress());
        order.setTotalPrice(0.0);

        // 3. Xử lý items
        List<OrderItem> itemEntities = dto.getItems() != null ? dto.getItems().stream().map(itemDTO -> {
            Product p = productRepo.findById(itemDTO.getProductId()).orElseThrow();
            OrderItem item = new OrderItem();
            item.setProduct(p);
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList()) : List.of();

        order.setOrderItems(itemEntities);
        double total = itemEntities.stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();
        order.setTotalPrice(total);

        // Save (cascade lưu luôn items)
        Order saved = orderRepo.save(order);
        return toDTO(saved);
    }

    @Override
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepo.findById(id).orElseThrow();
        return toDTO(order);
    }

    @Override
    public List<OrderDTO> findByUser(Long userId) {
        return orderRepo.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> findByStatus(String status) {
        return orderRepo.findByStatus(status).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getAllOrders() {
        return orderRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void updateStatus(Long id, String status) {
        Order order = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));
        String oldStatus = order.getStatus();

        // Logic trừ stock khi chuyển sang DELIVERED
        if ("delivered".equalsIgnoreCase(status) && !"delivered".equalsIgnoreCase(oldStatus)) {
            // Trừ stock cho tất cả items trong order
            for (OrderItem item : order.getOrderItems()) {
                productService.decreaseStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        // Logic hoàn stock khi chuyển sang CANCELLED (nếu đã delivered trước đó)
        if ("cancelled".equalsIgnoreCase(status) && "delivered".equalsIgnoreCase(oldStatus)) {
            // Hoàn lại stock
            for (OrderItem item : order.getOrderItems()) {
                productService.restoreStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        order.setStatus(status);
        orderRepo.save(order);
    }

    @Override
    public void cancelOrder(Long id) {
        Order order = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // Nếu đơn đã delivered thì hoàn stock khi cancel
        if ("delivered".equalsIgnoreCase(order.getStatus())) {
            for (OrderItem item : order.getOrderItems()) {
                productService.restoreStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        order.setStatus("cancelled");
        orderRepo.save(order);
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepo.deleteById(id);
    }
}
