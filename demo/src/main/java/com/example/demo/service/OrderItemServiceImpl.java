package com.example.demo.service;

import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.OrderItem;
import com.example.demo.model.entity.Product;
import com.example.demo.repo.OrderItemRepository;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository itemRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    @Override
    public OrderItemDTO addOrderItem(OrderItemDTO dto) {
        Order order = orderRepo.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found!"));
        Product product = productRepo.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found!"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        OrderItem saved = itemRepo.save(item);

        recalculateOrderTotal(dto.getOrderId());
        return toDTO(saved); // mapping đúng id sang DTO!

    }

    @Override
    public List<OrderItemDTO> getItemsByOrder(Long orderId) {
        return itemRepo.findByOrderId(orderId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderItemDTO getItemById(Long id) {
        OrderItem item = itemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("OrderItem not found!"));
        return toDTO(item);
    }

    @Override
    public OrderItemDTO updateOrderItem(Long id, OrderItemDTO dto) {
        OrderItem item = itemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("OrderItem not found!"));
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        itemRepo.save(item);

        recalculateOrderTotal(item.getOrder().getId());
        return toDTO(item);
    }

    @Override
    public void deleteOrderItem(Long id) {
        OrderItem item = itemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("OrderItem not found!"));
        Long orderId = item.getOrder().getId();
        itemRepo.delete(item);
        recalculateOrderTotal(orderId);
    }

    @Override
    public void recalculateOrderTotal(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found!"));
        double total = order.getOrderItems().stream()
                .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                .sum();
        order.setTotalPrice(total);
        orderRepo.save(order);
    }

    private OrderItemDTO toDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .orderId(item.getOrder().getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getQuantity() * item.getUnitPrice())
                .build();
    }
}
