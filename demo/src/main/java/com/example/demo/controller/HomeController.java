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
            session.setAttribute("cart", cart);
        }

        model.addAttribute("cart", session.getAttribute("cart"));
        return "home";
    }
}

