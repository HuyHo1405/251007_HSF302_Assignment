package com.example.demo.service;

import com.example.demo.model.dto.OrderDTO;
import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.OrderItem;
import com.example.demo.model.entity.Payment;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.User;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.OrderItemRepository;
import com.example.demo.repo.ProductRepository;
import com.example.demo.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final ProductService productService;
    private final PaymentService paymentService; // THÊM PaymentService

    // Phí vận chuyển mặc định
    private static final double DEFAULT_SHIPPING_FEE = 30000.0;

    // Helper convert
    private OrderDTO toDTO(Order order) {
        System.out.println("Converting Order #" + order.getId() + " to DTO");
        System.out.println("Order items count: " + (order.getOrderItems() != null ? order.getOrderItems().size() : "null"));

        List<OrderItemDTO> items = List.of();
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            items = order.getOrderItems().stream().map(item -> {
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
        }

        // Tính totalPrice từ items nếu order.totalPrice = 0 hoặc null
        Double totalPrice = order.getTotalPrice();
        if (totalPrice == null || totalPrice == 0.0) {
            double subtotal = items.stream()
                    .mapToDouble(OrderItemDTO::getSubtotal)
                    .sum();
            Double shippingFee = order.getShippingFee() != null ? order.getShippingFee() : DEFAULT_SHIPPING_FEE;
            totalPrice = subtotal + shippingFee;
        }

        System.out.println("Total price: " + totalPrice + ", Items count: " + items.size());

        User orderUser = order.getUser();
        String userPhone = orderUser != null ? orderUser.getPhoneNumber() : null;
        String userEmail = orderUser != null ? orderUser.getEmailAddress() : null;

        String paymentStatus = null;
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            paymentStatus = order.getPayments().stream()
                    .sorted(Comparator.comparing(Payment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(Payment::getStatus)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        return OrderDTO.builder()
                .id(order.getId())
                .userId(orderUser != null ? orderUser.getId() : null)
                .userFullName(orderUser != null ? orderUser.getFullName() : null)
                .userPhoneNumber(userPhone)
                .userEmail(userEmail)
                .status(order.getStatus())
                .paymentStatus(paymentStatus)
                .totalPrice(totalPrice)
                .shippingFee(order.getShippingFee() != null ? order.getShippingFee() : DEFAULT_SHIPPING_FEE)
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt() + "")
                .updatedAt(order.getUpdatedAt() + "")
                .items(items)
                .build();
    }

    @Override
    public OrderDTO createOrder(OrderDTO dto) {
        // 1. BẮT BUỘC phải set user TỪ userId
        if (dto.getUserId() == null) throw new IllegalArgumentException("UserId không được null");
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy userId: " + dto.getUserId()));

        // 2. VALIDATE STOCK TRƯỚC KHI TẠO ORDER
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (OrderItemDTO itemDTO : dto.getItems()) {
                if (!productService.isStockAvailable(itemDTO.getProductId(), itemDTO.getQuantity())) {
                    Product p = productRepo.findById(itemDTO.getProductId()).orElseThrow();
                    throw new RuntimeException("Sản phẩm '" + p.getName() + "' không đủ hàng trong kho");
                }
            }
        }

        Order order = new Order();
        order.setUser(user);

        // 3. Set status = "confirmed" (KHÔNG PHẢI "pending")
        order.setStatus("confirmed");
        order.setShippingAddress(dto.getShippingAddress());
        order.setTotalPrice(0.0);

        // 4. Xử lý items
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

        // 5. Tính tổng tiền sản phẩm
        double subtotal = itemEntities.stream()
                .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                .sum();

        // 6. Lấy phí vận chuyển từ DTO hoặc dùng mặc định
        Double shippingFee = dto.getShippingFee() != null ? dto.getShippingFee() : DEFAULT_SHIPPING_FEE;
        order.setShippingFee(shippingFee);

        // 7. Tổng tiền = tạm tính + phí ship
        order.setTotalPrice(subtotal + shippingFee);

        // Save (cascade lưu luôn items)
        Order saved = orderRepo.save(order);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepo.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
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

        // VALIDATE LUỒNG STATUS (CHỈ CHO PHÉP THEO ĐÚNG FLOW)
        validateStatusTransition(oldStatus, status);

        // LOGIC TRỪ STOCK KHI CHUYỂN SANG SHIPPING
        if ("shipping".equalsIgnoreCase(status) && "paid".equalsIgnoreCase(oldStatus)) {
            for (OrderItem item : order.getOrderItems()) {
                productService.decreaseStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        order.setStatus(status);
        orderRepo.save(order);
    }

    @Override
    public void cancelOrder(Long id) {
        Order order = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));
        String currentStatus = order.getStatus();

        // CHỈ CHO PHÉP CANCEL Ở CONFIRMED VÀ PAID
        if (!"confirmed".equalsIgnoreCase(currentStatus) && !"paid".equalsIgnoreCase(currentStatus)) {
            throw new RuntimeException("Không thể hủy đơn hàng ở trạng thái: " + currentStatus);
        }

        // REFUND: Nếu đã paid thì gọi VNPay refund API
        if ("paid".equalsIgnoreCase(currentStatus)) {
            try {
                Map<String, String> refundResult = paymentService.refundVnpayPayment(
                    order.getId(),
                    "Customer cancellation"
                );

                System.out.println("=== ORDER CANCELLED - REFUND INITIATED ===");
                System.out.println("Order ID: " + order.getId());
                System.out.println("Refund Status: " + refundResult.get("status"));
                System.out.println("Refund Message: " + refundResult.get("message"));
                System.out.println("Refund Code: " + refundResult.get("refundCode"));
                System.out.println("Amount: " + refundResult.get("amount") + " VND");
                System.out.println("==========================================");
            } catch (Exception e) {
                System.err.println("=== REFUND FAILED ===");
                System.err.println("Error: " + e.getMessage());
                System.err.println("Order will still be cancelled, but refund needs manual processing");
                System.err.println("=====================");
            }
        }

        order.setStatus("cancelled");
        orderRepo.save(order);
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepo.deleteById(id);
    }

    /**
     * VALIDATE STATUS TRANSITION - CHỈ CHO PHÉP THEO ĐÚNG LUỒNG
     * confirmed -> paid -> shipping -> delivered
     * confirmed -> cancelled (OK)
     * paid -> cancelled (OK)
     * shipping -> cancelled (KHÔNG ĐƯỢC PHÉP)
     * cancelled -> bất kỳ (KHÔNG ĐƯỢC PHÉP)
     * delivered -> bất kỳ (KHÔNG ĐƯỢC PHÉP)
     */
    private void validateStatusTransition(String oldStatus, String newStatus) {
        // Chuẩn hóa về lowercase để so sánh
        String oldLower = oldStatus != null ? oldStatus.toLowerCase() : "";
        String newLower = newStatus != null ? newStatus.toLowerCase() : "";

        // RULE 1: Nếu status giống nhau thì không cần validate
        if (oldLower.equals(newLower)) {
            return;
        }

        // RULE 2: Nếu đã cancelled hoặc delivered thì KHÔNG CHO ĐỔI STATUS NỮA (final states)
        if ("cancelled".equals(oldLower) || "delivered".equals(oldLower)) {
            throw new RuntimeException("Không thể thay đổi trạng thái từ: " + oldStatus + ". Đây là trạng thái cuối cùng.");
        }

        // RULE 3: Validate luồng hợp lệ theo từng trạng thái
        boolean isValid = false;

        switch (oldLower) {
            case "confirmed":
                // confirmed chỉ có thể chuyển sang: paid hoặc cancelled
                isValid = "paid".equals(newLower) || "cancelled".equals(newLower);
                break;

            case "paid":
                // paid chỉ có thể chuyển sang: shipping hoặc cancelled
                isValid = "shipping".equals(newLower) || "cancelled".equals(newLower);
                break;

            case "shipping":
                // shipping CHỈ có thể chuyển sang: delivered (KHÔNG được cancel)
                isValid = "delivered".equals(newLower);
                break;

            default:
                isValid = false;
        }

        if (!isValid) {
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + oldStatus + "' sang '" + newStatus + "'. Vui lòng kiểm tra lại luồng: confirmed → paid → shipping → delivered");
        }
    }
}
