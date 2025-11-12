package com.example.demo.controller;

import com.example.demo.model.dto.OrderDTO;
import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.entity.OrderItem;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.User;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final ProductService productService; // Đã tiêm qua constructor
    private final OrderService orderService;

    private static final double SHIPPING_FEE = 30000.0;

    @GetMapping("")
    public String viewCart(HttpSession session, Model model) {
        Map<Long, OrderItemDTO> cart = (Map<Long, OrderItemDTO>) session.getAttribute("cart");
        if (cart == null) cart = new HashMap<>();
        // Compute total server-side and pass it to the view. Handle possible null subtotals.
        java.util.Collection<OrderItemDTO> items = cart.values();
        double total = items.stream()
                .mapToDouble(i -> i.getSubtotal() != null ? i.getSubtotal() : 0.0)
                .sum();
        model.addAttribute("cart", items);
        model.addAttribute("total", total);
        return "cart/view";
    }

    @GetMapping("/checkout-form")
    public String showOrderCreateForm(HttpSession session, Model model, @AuthenticationPrincipal User user) {
        Map<Long, OrderItemDTO> cart = (Map<Long, OrderItemDTO>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            model.addAttribute("error", "Giỏ hàng trống!");
            return "cart/view";
        }
        // Build OrderDTO để truyền sang view
        OrderDTO orderDTO = OrderDTO.builder()
                .userId(user.getId())
                .userFullName(user.getFullName())
                .status("confirmed") // Sửa từ "PENDING" sang "confirmed"
                .shippingAddress(user.getFullAddress())
                .shippingFee(SHIPPING_FEE)
                .items(new ArrayList<>(cart.values()))
                .build();
        java.util.Collection<OrderItemDTO> items = cart.values();
        double subtotal = items.stream()
                .mapToDouble(i -> i.getSubtotal() != null ? i.getSubtotal() : 0.0)
                .sum();

        double totalWithShipping = subtotal + SHIPPING_FEE;
        String userAddress = user.getFullAddress();

        model.addAttribute("orderSubtotal", subtotal); // Tạm tính (chưa có phí ship)
        model.addAttribute("shippingFee", SHIPPING_FEE); // Phí vận chuyển
        model.addAttribute("orderTotal", totalWithShipping); // Tổng thanh toán (đã bao gồm phí ship)
        model.addAttribute("order", orderDTO); // Truyền đúng là DTO!
        model.addAttribute("userAddress", userAddress);
        model.addAttribute("userAddress", user.getFullAddress());
        return "orders/create";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam("quantity") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        // Lấy giỏ hàng từ session, nếu chưa có thì tạo mới
        Map<Long, OrderItemDTO> cart = (Map<Long, OrderItemDTO>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
        }

        try {
            // Tìm sản phẩm
            Product product = productService.getProductById(productId);

            // Kiểm tra đã có item đó trong giỏ chưa
            if (cart.containsKey(productId)) {
                OrderItemDTO item = cart.get(productId);
                item.setQuantity(item.getQuantity() + quantity);
                item.setSubtotal(item.getUnitPrice() * item.getQuantity()); // Cập nhật subtotal
            } else {
                OrderItemDTO newItem = new OrderItemDTO();
                newItem.setProductId(product.getId());
                newItem.setProductName(product.getName());
                newItem.setUnitPrice(product.getUnitPrice());
                newItem.setQuantity(quantity);
                newItem.setSubtotal(product.getUnitPrice() * quantity);
                cart.put(productId, newItem);
            }

            // Cập nhật lại session
            session.setAttribute("cart", cart);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/home";
        }

        // Redirect về trang giỏ hàng hoặc homepage tuỳ bạn
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateCart(@RequestParam("productId") Long productId,
                             @RequestParam("quantity") int quantity,
                             HttpSession session) {
        Map<Long, OrderItemDTO> cart = (Map<Long, OrderItemDTO>) session.getAttribute("cart");
        if (cart != null && cart.containsKey(productId)) {
            OrderItemDTO item = cart.get(productId);
            item.setQuantity(quantity);
             item.setSubtotal(item.getUnitPrice() * quantity);
            session.setAttribute("cart", cart);
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam("productId") Long productId, HttpSession session) {
        Map<Long, OrderItemDTO> cart = (Map<Long, OrderItemDTO>) session.getAttribute("cart");
        if (cart != null) {
            cart.remove(productId);
            session.setAttribute("cart", cart);
        }
        return "redirect:/cart";
    }


    // 5. Xóa toàn bộ giỏ hàng
    @PostMapping("/clear")
    public String clearCart(HttpSession session) {
        session.removeAttribute("cart");
        return "redirect:/cart";
    }
}