package com.example.demo.controller;

import com.example.demo.model.dto.OrderDTO;
import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.dto.PaymentDTO;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.User;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.ProductRepository;
import com.example.demo.repo.UserRepository;
import com.example.demo.service.OrderItemService;
import com.example.demo.service.OrderService;
import com.example.demo.service.PaymentServiceImpl;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ProductRepository productRepo;
    private final PaymentServiceImpl paymentService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private static final double SHIPPING_FEE = 30000.0;

    // Xem đơn hàng - Tự động lọc theo role
    @GetMapping
    public String listOrders(@RequestParam(required = false) String status,
                             @AuthenticationPrincipal User currentUser,
                             Model model) {
        List<OrderDTO> orders;

        // CUSTOMER chỉ xem đơn của mình, STAFF/ADMIN xem tất cả
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            orders = (status == null || status.isEmpty())
                    ? orderService.findByUser(currentUser.getId())
                    : orderService.findByUser(currentUser.getId()).stream()
                    .filter(o -> {
                        System.out.println("Order status: '" + o.getStatus() + "', Filter status: '" + status + "'");
                        return o.getStatus().equalsIgnoreCase(status); // SỬA: Thêm equalsIgnoreCase
                    })
                    .collect(Collectors.toList());
        } else {
            // STAFF/ADMIN xem tất cả đơn
            orders = (status == null || status.isEmpty())
                    ? orderService.getAllOrders()
                    : orderService.findByStatus(status);
        }

        System.out.println("Total orders found: " + orders.size()); // Debug log

        model.addAttribute("orders", orders);
        model.addAttribute("status", status);
        model.addAttribute("currentUser", currentUser);
        return "orders/list";
    }

    @GetMapping("/my")
    public String viewMyOrders(@RequestParam(required = false) String status,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        List<OrderDTO> myOrders = orderService.findByUser(currentUser.getId());

        if (status != null && !status.isBlank()) {
            String filter = status.trim();
            myOrders = myOrders.stream()
                    .filter(order -> order.getStatus() != null && order.getStatus().equalsIgnoreCase(filter))
                    .collect(Collectors.toList());
        }

        myOrders.sort(Comparator.comparing(OrderDTO::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        model.addAttribute("orders", myOrders);
        model.addAttribute("status", status);
        model.addAttribute("currentUser", currentUser);
        return "orders/my";
    }

    // Xem chi tiết đơn hàng
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        OrderDTO dto = orderService.getOrderById(id);

        // Check quyền: Customer chỉ xem đơn của mình, Staff/Admin xem tất cả
        if (currentUser.getRole() == User.Role.CUSTOMER && !dto.getUserId().equals(currentUser.getId())) {
            model.addAttribute("orderId", id);
            return "orders/not-owner";
        }

        // Danh sách các item (cho table lặp)
        model.addAttribute("items", dto.getItems() == null ? List.of() : dto.getItems());

        // Sản phẩm (dropdown, dùng form thêm)
        model.addAttribute("products", productRepo.findAll());

        // Thông tin đơn
        model.addAttribute("order", dto);

        List<PaymentDTO> paymentList = paymentService.getPaymentDTOsByOrderId(id); // SỬA ĐÂY!
        model.addAttribute("payments", paymentList);

        // Truyền 1 payment rỗng cho form tạo payment mới nếu muốn dùng binding
        model.addAttribute("payment", new PaymentDTO());

        // CHỐT QUAN TRỌNG: truyền một OrderItemDTO rỗng cho form thêm/sửa
        model.addAttribute("orderItem", new OrderItemDTO());

        model.addAttribute("currentUser", currentUser);

        return "orders/detail"; // Thymeleaf template
    }

    // Tạo đơn mua hàng (POST form)
    @PostMapping("/create")
    @SuppressWarnings("unchecked")
    public String createOrder(@ModelAttribute OrderDTO orderDTO,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        try {
            // Set user ID from current user
            orderDTO.setUserId(currentUser.getId());
            Map<Long, OrderItemDTO> cart = (Map<Long, OrderItemDTO>) session.getAttribute("cart");
            if (cart == null || cart.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống, không thể tạo đơn hàng.");
                return "redirect:/cart";
            }
            orderDTO.setItems(new ArrayList<>(cart.values()));

            // Tính tổng tiền sản phẩm
            double subtotal = orderDTO.getItems().stream()
                    .mapToDouble(item -> {
                        if (item.getSubtotal() != null) {
                            return item.getSubtotal();
                        }
                        double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0.0;
                        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                        return unitPrice * quantity;
                    })
                    .sum();

            // Cộng thêm phí vận chuyển vào tổng tiền
            double totalWithShipping = subtotal + SHIPPING_FEE;

            // Set shippingFee và totalPrice
            orderDTO.setShippingFee(SHIPPING_FEE);
            orderDTO.setTotalPrice(totalWithShipping);

            OrderDTO created = orderService.createOrder(orderDTO);
            if (session != null) {
                session.removeAttribute("cart");
            }
            redirectAttributes.addFlashAttribute("success", "Tạo đơn hàng thành công!");
            return "redirect:/orders/" + created.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    // Cập nhật trạng thái đơn hàng (staff/admin)
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam String status,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        // Check quyền: chỉ STAFF và ADMIN mới update được status
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền cập nhật trạng thái đơn hàng");
            return "redirect:/orders/" + id;
        }

        try {
            orderService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    // Cancel đơn (customer chỉ cancel đơn của mình)
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            OrderDTO order = orderService.getOrderById(id);

            // Customer chỉ cancel đơn của mình
            if (currentUser.getRole() == User.Role.CUSTOMER && !order.getUserId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền hủy đơn hàng này");
                return "redirect:/orders/my";
            }

            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    // Xóa đơn (staff/admin only)
//    @PostMapping("/{id}/delete")
//    public String deleteOrder(@PathVariable Long id,
//                             @AuthenticationPrincipal User currentUser,
//                             RedirectAttributes redirectAttributes) {
//        // Check quyền: chỉ STAFF và ADMIN mới xóa được
//        if (currentUser.getRole() == User.Role.CUSTOMER) {
//            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa đơn hàng");
//            return "redirect:/orders/my";
//        }
//
//        try {
//            orderService.deleteOrder(id);
//            redirectAttributes.addFlashAttribute("success", "Xóa đơn hàng thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
//        }
//        return "redirect:/orders";
//    }

    @GetMapping("/{orderId}/items/{itemId}/edit")
    public String showEditItem(@PathVariable Long orderId,
                              @PathVariable Long itemId,
                              @AuthenticationPrincipal User currentUser,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Check quyền: chỉ STAFF và ADMIN
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền sửa order item");
            return "redirect:/orders/" + orderId;
        }

        OrderItemDTO dto = orderItemService.getItemById(itemId);
        model.addAttribute("orderItem", dto);
        model.addAttribute("products", productRepo.findAll());
        model.addAttribute("currentUser", currentUser);
        return "order_items/edit";
    }

    @PostMapping("/{orderId}/items/{itemId}/edit")
    public String updateOrderItem(@PathVariable Long orderId,
                                 @PathVariable Long itemId,
                                 @ModelAttribute OrderItemDTO dto,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        // Check quyền: chỉ STAFF và ADMIN
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền sửa order item");
            return "redirect:/orders/" + orderId;
        }

        try {
            dto.setOrderId(orderId);
            orderItemService.updateOrderItem(itemId, dto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật item thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/orders/" + orderId;
    }

    @PostMapping("/{orderId}/items/{itemId}/delete")
    public String deleteOrderItem(@PathVariable Long orderId,
                                 @PathVariable Long itemId,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        // Check quyền: chỉ STAFF và ADMIN
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa order item");
            return "redirect:/orders/" + orderId;
        }

        try {
            orderItemService.deleteOrderItem(itemId);
            redirectAttributes.addFlashAttribute("success", "Xóa item thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/orders/" + orderId;
    }

    // Tạo payment mới (GET form)
    @GetMapping("/{orderId}/payments/create")
    public String createPaymentForm(@PathVariable Long orderId,
                                   @AuthenticationPrincipal User currentUser,
                                   Model model) {
        PaymentDTO payment = new PaymentDTO();
        payment.setOrderId(orderId);
        model.addAttribute("payment", payment);
        model.addAttribute("currentUser", currentUser);
        return "orders/payment-create"; // Thymeleaf template
    }

    @GetMapping("/statistics")
    public String viewStatistics(@AuthenticationPrincipal User currentUser,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        // Chỉ admin được xem
        if (currentUser.getRole() != User.Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem thống kê");
            return "redirect:/orders";
        }

        // Lấy tất cả đơn hàng (Bạn đã có)
        List<OrderDTO> allOrders = orderService.getAllOrders();

        // === 1. BỔ SUNG DỮ LIỆU CHO 4 THẺ (Sửa lại logic cho khớp) ===

        // Chỉ tính doanh thu của đơn "delivered"
        double totalRevenue = allOrders.stream()
                .filter(o -> "delivered".equals(o.getStatus()))
                .mapToDouble(OrderDTO::getTotalPrice)
                .sum();

        long totalOrders = allOrders.size();

        long completedOrders = allOrders.stream()
                .filter(o -> "delivered".equals(o.getStatus()))
                .count();

        // Đơn đang xử lý là (Pending, Paid, Shipping)
        long pendingOrders = allOrders.stream()
                .filter(o -> "pending".equals(o.getStatus()) || "paid".equals(o.getStatus()) || "shipping".equals(o.getStatus()))
                .count();

        // === 2. BỔ SUNG DỮ LIỆU CHO BIỂU ĐỒ TRÒN (statusCounts) ===
        Map<String, Long> statusCounts = allOrders.stream()
                .collect(Collectors.groupingBy(
                        OrderDTO::getStatus,
                        Collectors.counting()
                ));

        // === 3. BỔ SUNG DỮ LIỆU CHO BIỂU ĐỒ CỘT (monthlyRevenue) ===
        // Chú ý: Đây là phần phức tạp nhất, bạn nên tạo 1 hàm trong Service
        // để truy vấn CSDL cho tối ưu.
        // Tạm thời, tôi sẽ hardcode dữ liệu MẪU để bạn thấy biểu đồ chạy:
        List<Double> monthlyRevenue = List.of(
                1500000.0, 3200000.0, 2100000.0, 4500000.0, 4200000.0, 6000000.0
        );
        // Khi làm thật, bạn hãy thay thế bằng:
        // List<Double> monthlyRevenue = orderService.getMonthlyRevenueLast6Months();


        // === 4. BỔ SUNG DỮ LIỆU CHO BẢNG (recentOrders) ===
        List<OrderDTO> recentOrders = allOrders.stream()
                .sorted(Comparator.comparing(OrderDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10) // Lấy 10 đơn hàng mới nhất
                .collect(Collectors.toList());


        // === 5. THÊM TẤT CẢ VÀO MODEL ===

        // Dữ liệu cho 4 thẻ
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("completedOrders", completedOrders); // ĐÃ THÊM
        model.addAttribute("pendingOrders", pendingOrders);     // ĐÃ THÊM

        // Dữ liệu cho 2 biểu đồ
        model.addAttribute("statusCounts", statusCounts);       // ĐÃ THÊM
        model.addAttribute("monthlyRevenue", monthlyRevenue);   // ĐÃ THÊM

        // Dữ liệu cho bảng
        model.addAttribute("recentOrders", recentOrders);       // ĐÃ THÊM

        // Các dữ liệu cũ bạn có (vẫn giữ lại)
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalCustomers", userRepository.countByRole(User.Role.CUSTOMER));
        model.addAttribute("topProducts", orderRepository.findTop10Products().stream().limit(10).collect(Collectors.toList()));
        model.addAttribute("currentUser", currentUser);

        return "orders/statistics";
    }

    @PostMapping("/{orderId}/items/add")
    public String addOrderItem(@PathVariable Long orderId,
                               @RequestParam Long productId,
                               @RequestParam Integer quantity,
                               @AuthenticationPrincipal User currentUser,
                               RedirectAttributes redirectAttributes) {
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thêm sản phẩm vào đơn hàng.");
            return "redirect:/orders/" + orderId;
        }

        if (quantity == null || quantity < 1) {
            redirectAttributes.addFlashAttribute("error", "Số lượng phải lớn hơn 0.");
            return "redirect:/orders/" + orderId;
        }

        try {
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));

            OrderItemDTO dto = OrderItemDTO.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .unitPrice(product.getUnitPrice() != null ? product.getUnitPrice() : 0.0)
                    .build();

            orderItemService.addOrderItem(dto);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm vào đơn hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể thêm sản phẩm: " + e.getMessage());
        }

        return "redirect:/orders/" + orderId;
    }
}
