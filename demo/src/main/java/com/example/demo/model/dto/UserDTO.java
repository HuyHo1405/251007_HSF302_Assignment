package com.example.demo.model.dto;

import com.example.demo.model.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private Long id;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Email Không được để trống")
    private String emailAddress;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    private User.Role role;

    private LocalDateTime  createdAt;

    private Long orderCount;
}
