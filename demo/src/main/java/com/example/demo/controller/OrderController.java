package com.example.demo.controller;

import com.example.demo.model.dto.OrderDTO;
import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.dto.PaymentDTO;
import com.example.demo.repo.ProductRepository;
import com.example.demo.service.OrderItemService;
import com.example.demo.service.OrderService;
import com.example.demo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ProductRepository productRepo;
    private final PaymentService paymentService;

    // Xem tất cả đơn (staff/admin, có thể filter status)
    @GetMapping
    public String listOrders(@RequestParam(required = false) String status, Model model) {
        List<OrderDTO> orders = (status == null)
                ? orderService.getAllOrders()
                : orderService.findByStatus(status);
        model.addAttribute("orders", orders);
        return "orders/list"; // Thymeleaf template
    }

    // Xem đơn hàng của tôi (customer)
    @GetMapping("/my")
    public String myOrders(@RequestParam Long userId, Model model) {
        List<OrderDTO> orders = orderService.findByUser(userId);
        model.addAttribute("orders", orders);
        return "orders/my"; // Thymeleaf template
    }

    // Xem chi tiết đơn hàng
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        OrderDTO dto = orderService.getOrderById(id);

        // Danh sách các item (cho table lặp)
        model.addAttribute("items", dto.getItems() == null ? List.of() : dto.getItems());

            // Sản phẩm (dropdown, dùng form thêm)
        model.addAttribute("products", productRepo.findAll());

        // Thông tin đơn
        model.addAttribute("order", dto);

        List<PaymentDTO> paymentList = paymentService.getPaymentsByOrderId(id);
        model.addAttribute("payments", paymentList);

        // Truyền 1 payment rỗng cho form tạo payment mới nếu muốn dùng binding
        model.addAttribute("payment", new PaymentDTO());

        // CHỐT QUAN TRỌNG: truyền một OrderItemDTO rỗng cho form thêm/sửa
        model.addAttribute("orderItem", new OrderItemDTO());

        return "orders/detail"; // Thymeleaf template
    }

    // Tạo đơn mua hàng (GET form)
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("order", new OrderDTO());
        // cần load thêm dữ liệu products/customer nếu cần
        return "orders/create"; // Thymeleaf template
    }

    // Tạo đơn mua hàng (POST form)
    @PostMapping("/create")
    public String createOrder(@ModelAttribute OrderDTO orderDTO) {
        OrderDTO created = orderService.createOrder(orderDTO);
        return "redirect:/orders/" + created.getId();
    }

    // Cập nhật trạng thái đơn hàng (staff)
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        orderService.updateStatus(id, status);
        return "redirect:/orders/" + id;
    }

    // Cancel đơn (customer) hoặc xóa đơn (staff/admin)
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return "redirect:/orders";
    }

    @PostMapping("/{orderId}/items/add")
    public String addOrderItem(@PathVariable Long orderId, @ModelAttribute OrderItemDTO dto) {
        dto.setOrderId(orderId); // truyền id đơn đang thao tác
        orderItemService.addOrderItem(dto);
        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/{orderId}/items/{itemId}/edit")
    public String showEditItem(@PathVariable Long orderId, @PathVariable Long itemId, Model model) {
        OrderItemDTO dto = orderItemService.getItemById(itemId);
        model.addAttribute("orderItem", dto);
        model.addAttribute("products", productRepo.findAll());
        return "order_items/edit";
    }

    @PostMapping("/{orderId}/items/{itemId}/edit")
    public String updateOrderItem(@PathVariable Long orderId, @PathVariable Long itemId, @ModelAttribute OrderItemDTO dto) {
        dto.setOrderId(orderId);
        orderItemService.updateOrderItem(itemId, dto);
        return "redirect:/orders/" + orderId;
    }

    @PostMapping("/{orderId}/items/{itemId}/delete")
    public String deleteOrderItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        orderItemService.deleteOrderItem(itemId);
        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/{orderId}/payments")
    public String viewPayments(@PathVariable Long orderId, Model model) {
        List<PaymentDTO> payments = paymentService.getPaymentsByOrderId(orderId);
        model.addAttribute("payments", payments);
        model.addAttribute("orderId", orderId);
        return "orders/payments"; // Thymeleaf template
    }

    // Tạo payment mới (GET form)
    @GetMapping("/{orderId}/payments/create")
    public String createPaymentForm(@PathVariable Long orderId, Model model) {
        PaymentDTO payment = new PaymentDTO();
        payment.setOrderId(orderId);
        model.addAttribute("payment", payment);
        return "orders/payment-create"; // Thymeleaf template
    }

    // Tạo payment mới (POST form)
    @PostMapping("/{orderId}/payments/create")
    public String createPayment(@PathVariable Long orderId, @ModelAttribute PaymentDTO dto) {
        dto.setOrderId(orderId);
        paymentService.createPayment(dto);
        return "redirect:/orders/" + orderId;
    }

    // Hoàn thành payment (chuyển pending -> completed)
    @PostMapping("/{orderId}/payments/{paymentId}/complete")
    public String completePayment(@PathVariable Long orderId, @PathVariable Long paymentId) {
        paymentService.completePayment(paymentId);
        return "redirect:/orders/" + orderId;
    }
}
