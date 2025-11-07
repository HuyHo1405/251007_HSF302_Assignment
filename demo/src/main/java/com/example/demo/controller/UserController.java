package com.example.demo.controller;

import com.example.demo.model.dto.UserDTO;
import com.example.demo.model.entity.User;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.dto.UpdateUserDTO;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    // Xem thông tin hồ sơ
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User currentUser, Model model) {
        UserDTO userDTO = userService.getUserById(currentUser.getId());
        model.addAttribute("user", userDTO);
        model.addAttribute("currentUser", currentUser);
        return "user/profile";
    }

    // Form sửa hồ sơ
    @GetMapping("/profile/edit")
    public String editProfileForm(@AuthenticationPrincipal User currentUser, Model model) {
        UserDTO userDTO = userService.getUserById(currentUser.getId());
        
        UpdateUserDTO dto = UpdateUserDTO.builder()
                .fullName(userDTO.getFullName())
                .emailAddress(userDTO.getEmailAddress())
                .phoneNumber(userDTO.getPhoneNumber())
                .build();
        
        model.addAttribute("updateProfile", dto);
        model.addAttribute("currentUser", currentUser);
        return "user/editprofile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@AuthenticationPrincipal User currentUser,
                                @Valid @ModelAttribute("updateProfile") UpdateUserDTO dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            return "user/editprofile";
        }
        try {
            userService.updateProfile(currentUser.getId(), dto, currentUser.getId(), currentUser.getRole());
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
            return "redirect:/users/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("currentUser", currentUser);
            return "user/editprofile";
        }
    }

    // Xóa tài khoản (tự xóa hoặc admin/staff xóa)
    @PostMapping("/{userId}/delete")
    public String deleteUser(@PathVariable Long userId,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(userId, currentUser.getId(), currentUser.getRole());

            // Nếu user tự xóa tài khoản của mình, redirect về trang login
            if (userId.equals(currentUser.getId())) {
                return "redirect:/auth/logout";
            }

            redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công!");
            return "redirect:/users/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/users/list";
        }
    }

    // Danh sách người dùng (admin/staff) - mặc định hiển thị customers
    @GetMapping("/list")
    public String listUsers(@RequestParam(required = false) String role,
                            @AuthenticationPrincipal User currentUser,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        // Kiểm tra quyền
        if (!userService.hasPermission(currentUser.getRole(), "VIEW_ALL", "USER")) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem danh sách người dùng");
            return "redirect:/home";
        }

        try {
            // Lấy tất cả users
            List<UserDTO> allUsers = userService.getAllUsers();
            List<UserDTO> displayUsers = allUsers;

            // Nếu có filter theo role
            if (role != null && !role.isEmpty()) {
                User.Role requestedRole = User.Role.valueOf(role);
                displayUsers = userService.getUserByRole(requestedRole);
                model.addAttribute("selectedRole", role);
            }

            // Tính toán statistics
            long totalUsers = allUsers.size();
            long adminCount = allUsers.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();
            long staffCount = allUsers.stream().filter(u -> u.getRole() == User.Role.STAFF).count();
            long customerCount = allUsers.stream().filter(u -> u.getRole() == User.Role.CUSTOMER).count();

            model.addAttribute("users", displayUsers);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("adminCount", adminCount);
            model.addAttribute("staffCount", staffCount);
            model.addAttribute("customerCount", customerCount);
            model.addAttribute("currentUser", currentUser);

            return "user/userList";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Role không hợp lệ");
            return "redirect:/users/list";
        }
    }

    // Dashboard cho admin
    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal User currentUser, Model model) {
        if (currentUser.getRole() != User.Role.ADMIN) {
            return "redirect:/home";
        }

        model.addAttribute("currentUser", currentUser);
        return "user/adminDashboard";
    }

    // Dashboard cho user
    @GetMapping("/dashboard")
    public String userDashboard(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);

        if (currentUser.getRole() == User.Role.ADMIN) {
            return "user/adminDashboard";
        } else {
            return "user/userDashboard";
        }
    }
}
