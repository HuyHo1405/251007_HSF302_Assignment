package com.example.demo.controller;

import com.example.demo.model.dto.OrderDTO;
import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.dto.PaymentDTO;
import com.example.demo.model.entity.User;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.ProductRepository;
import com.example.demo.repo.UserRepository;
import com.example.demo.service.OrderItemService;
import com.example.demo.service.OrderService;
import com.example.demo.service.PaymentServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
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
                    .filter(o -> o.getStatus().equalsIgnoreCase(status)) // SỬA: equalsIgnoreCase
                    .collect(Collectors.toList());
        } else {
            // STAFF/ADMIN xem tất cả đơn
            orders = (status == null || status.isEmpty())
                    ? orderService.getAllOrders()
                    : orderService.findByStatus(status);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("status", status);
        model.addAttribute("currentUser", currentUser);
        return "orders/list";
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
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem đơn hàng này");
            return "redirect:/orders";
        }

        // Danh sách các item (cho table lặp)
        model.addAttribute("items", dto.getItems() == null ? List.of() : dto.getItems());

        // Sản phẩm (dropdown, dùng form thêm)
        model.addAttribute("products", productRepo.findAll());

        // Thông tin đơn
        model.addAttribute("order", dto);

        List<PaymentDTO> paymentList = paymentService.getPaymentDTOsByOrderId(id);
        model.addAttribute("payments", paymentList);

        // Truyền 1 payment rỗng cho form tạo payment mới nếu muốn dùng binding
        model.addAttribute("payment", new PaymentDTO());

        // CHỐT QUAN TRỌNG: truyền một OrderItemDTO rỗng cho form thêm/sửa
        model.addAttribute("orderItem", new OrderItemDTO());

        model.addAttribute("currentUser", currentUser);

        return "orders/detail"; // Thymeleaf template
    }

    // ===== SỬA: Tạo đơn mua hàng và REDIRECT đến VNPay =====
    @PostMapping("/create")
    public String createOrder(HttpSession session,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            // Lấy giỏ hàng từ session
            Map<Long, OrderItemDTO> cart = (Map<Long, OrderItemDTO>) session.getAttribute("cart");

            if (cart == null || cart.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống!");
                return "redirect:/cart";
            }

            // Tính tổng tiền
            double totalPrice = cart.values().stream()
                    .mapToDouble(OrderItemDTO::getSubtotal)
                    .sum();

            // Tạo OrderDTO
            OrderDTO orderDTO = OrderDTO.builder()
                    .userId(currentUser.getId())
                    .userFullName(currentUser.getFullName())
                    .status("PENDING") // Trạng thái ban đầu là PENDING
                    .totalPrice(totalPrice) // QUAN TRỌNG: Set tổng tiền
                    .items(new ArrayList<>(cart.values()))
                    .build();

            // Tạo order
            OrderDTO createdOrder = orderService.createOrder(orderDTO);

            // Xóa giỏ hàng sau khi tạo đơn
            session.removeAttribute("cart");

            // ===== REDIRECT ĐẾN TRANG CHỌN PHƯƠNG THỨC THANH TOÁN =====
            redirectAttributes.addFlashAttribute("success", "Đơn hàng đã được tạo! Vui lòng chọn phương thức thanh toán.");
            return "redirect:/payments/create/" + createdOrder.getId();
            // ===== KẾT THÚC =====

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

    // Cancel đơn (customer chỉ cancel đơn của mình và chỉ khi PENDING)
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            OrderDTO order = orderService.getOrderById(id);

            // ===== LOGIC: Customer chỉ hủy đơn của mình khi PENDING =====
            if (currentUser.getRole() == User.Role.CUSTOMER) {
                // Check: Có phải đơn của mình không?
                if (!order.getUserId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "Bạn không có quyền hủy đơn hàng này");
                    return "redirect:/orders";
                }
                // Check: Đơn có đang PENDING không?
                if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
                    redirectAttributes.addFlashAttribute("error", "Chỉ có thể hủy đơn hàng đang ở trạng thái PENDING");
                    return "redirect:/orders/" + id;
                }
            }
            // STAFF/ADMIN có thể hủy bất kỳ đơn nào (không check)
            // ===== KẾT THÚC =====

            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    // Xóa đơn (staff/admin only)
    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        // Check quyền: chỉ STAFF và ADMIN mới xóa được
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa đơn hàng");
            return "redirect:/orders";
        }

        try {
            orderService.deleteOrder(id);
            redirectAttributes.addFlashAttribute("success", "Xóa đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/orders";
    }

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

        // Lấy tất cả đơn hàng
        List<OrderDTO> allOrders = orderService.getAllOrders();

        // Chỉ tính doanh thu của đơn "delivered"
        double totalRevenue = allOrders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(OrderDTO::getTotalPrice)
                .sum();

        long totalOrders = allOrders.size();

        long completedOrders = allOrders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getStatus()))
                .count();

        // Đơn đang xử lý là (Pending, Paid, Shipping)
        long pendingOrders = allOrders.stream()
                .filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())
                        || "PAID".equalsIgnoreCase(o.getStatus())
                        || "SHIPPING".equalsIgnoreCase(o.getStatus()))
                .count();

        Map<String, Long> statusCounts = allOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().toUpperCase(),
                        Collectors.counting()
                ));

        // Dữ liệu mẫu cho biểu đồ doanh thu
        List<Double> monthlyRevenue = List.of(
                1500000.0, 3200000.0, 2100000.0, 4500000.0, 4200000.0, 6000000.0
        );

        List<OrderDTO> recentOrders = allOrders.stream()
                .sorted(Comparator.comparing(OrderDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());

        // Dữ liệu cho 4 thẻ
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("pendingOrders", pendingOrders);

        // Dữ liệu cho 2 biểu đồ
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("monthlyRevenue", monthlyRevenue);

        // Dữ liệu cho bảng
        model.addAttribute("recentOrders", recentOrders);

        // Các dữ liệu khác
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalCustomers", userRepository.countByRole(User.Role.CUSTOMER));
        model.addAttribute("topProducts", orderRepository.findTop10Products().stream().limit(10).collect(Collectors.toList()));
        model.addAttribute("currentUser", currentUser);

        return "orders/statistics";
    }
}