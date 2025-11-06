package com.example.demo.controller;

import com.example.demo.model.dto.OrderItemDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;


@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {

        if (session.getAttribute("cart") == null) {
            Map<Long, OrderItemDTO> cart = new HashMap<>();

            // === BẮT ĐẦU FAKE DATA ===
            // (Giả sử OrderItemDTO của bạn có các hàm setter bên dưới)

            // Sản phẩm 1
            OrderItemDTO item1 = new OrderItemDTO();
            item1.setProductId(1L);
            item1.setProductName("Sản phẩm TEST 1 (Fake)");
            item1.setQuantity(2);
            item1.setUnitPrice(150000.0);
            item1.setSubtotal(300000.0); // 2 * 150000

            // Sản phẩm 2
            OrderItemDTO item2 = new OrderItemDTO();
            item2.setProductId(2L);
            item2.setProductName("Sản phẩm TEST 2 (Fake)");
            item2.setQuantity(1);
            item2.setUnitPrice(500000.0);
            item2.setSubtotal(500000.0); // 1 * 500000

            // Thêm vào giỏ hàng
            cart.put(item1.getProductId(), item1);
            cart.put(item2.getProductId(), item2);

            // === KẾT THÚC FAKE DATA ===

            session.setAttribute("cart", cart);
        }

        model.addAttribute("cart", session.getAttribute("cart"));
        return "home";
    }
}

