package com.example.demo.controller;

import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model, Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (isAuthenticated && session.getAttribute("cart") == null) {
            Map<Long, OrderItemDTO> cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("topProducts", productService.getTopBestSellers(10));
        model.addAttribute("cart", session.getAttribute("cart"));
        return "home";
    }
}

