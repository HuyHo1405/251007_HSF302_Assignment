package com.example.demo.controller;

import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.Payment;
import com.example.demo.service.PaymentService;
import com.example.demo.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    /**
     * Hiển thị trang chọn phương thức thanh toán
     */
    @GetMapping("/create/{orderId}")
    public String showPaymentOptions(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            model.addAttribute("order", order);
            return "orders/payment-create";
        } catch (Exception e) {
            log.error("Error showing payment options: ", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/orders/my";
        }
    }

    /**
     * Tạo thanh toán VNPay và redirect đến VNPay
     */
    @PostMapping("/vnpay/create")
    public String createVnpayPayment(@RequestParam Long orderId, RedirectAttributes redirectAttributes) {
        try {
            String paymentUrl = paymentService.createVnpayPayment(orderId);
            log.info("Created VNPay payment URL for order {}", orderId);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            log.error("Error creating VNPay payment: ", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo thanh toán: " + e.getMessage());
            return "redirect:/orders/detail/" + orderId;
        }
    }

    /**
     * VNPay callback sau khi thanh toán (ReturnUrl)
     */
    @GetMapping("/vnpay/return")
    public String vnpayReturn(@RequestParam Map<String, String> params, Model model) {
        try {
            Map<String, String> result = paymentService.handleVnpayReturn(params);

            model.addAttribute("status", result.get("status"));
            model.addAttribute("message", result.get("message"));
            model.addAttribute("orderCode", result.get("orderCode"));

            if ("success".equals(result.get("status"))) {
                Payment payment = paymentService.getPaymentByOrderCode(result.get("orderCode"));
                model.addAttribute("payment", payment);
                model.addAttribute("order", payment.getOrder());
                return "orders/payment-success";
            } else {
                return "orders/payment-failed";
            }

        } catch (Exception e) {
            log.error("Error handling VNPay return: ", e);
            model.addAttribute("status", "error");
            model.addAttribute("message", "Lỗi xử lý thanh toán: " + e.getMessage());
            return "orders/payment-failed";
        }
    }

    /**
     * Tạo thanh toán COD
     */
    @PostMapping("/cod/create")
    public String createCODPayment(@RequestParam Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.createCODPayment(orderId);
            log.info("Created COD payment for order {}", orderId);
            redirectAttributes.addFlashAttribute("success", "Đã tạo đơn COD! Vui lòng chuẩn bị tiền mặt khi nhận hàng.");
            return "redirect:/orders/detail/" + orderId;
        } catch (Exception e) {
            log.error("Error creating COD payment: ", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo thanh toán COD: " + e.getMessage());
            return "redirect:/orders/detail/" + orderId;
        }
    }

    /**
     * Simulate VNPay success (cho test)
     */
    @PostMapping("/vnpay/simulate/{orderCode}")
    public String simulateVnpaySuccess(@PathVariable String orderCode, RedirectAttributes redirectAttributes) {
        try {
            Map<String, String> result = paymentService.simulateVnpaySuccess(orderCode);

            if ("success".equals(result.get("status"))) {
                redirectAttributes.addFlashAttribute("success", "Simulate thanh toán thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", result.get("message"));
            }

            Payment payment = paymentService.getPaymentByOrderCode(orderCode);
            return "redirect:/orders/detail/" + payment.getOrder().getId();

        } catch (Exception e) {
            log.error("Error simulating VNPay success: ", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi simulate: " + e.getMessage());
            return "redirect:/orders/my";
        }
    }
}
