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

    private String province;
    private String district;
    private String ward;
    private String addressDetail;

    private User.Role role;

    private LocalDateTime  createdAt;

    private Long orderCount;

    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        if (addressDetail != null && !addressDetail.isEmpty()) {
            fullAddress.append(addressDetail);
        }
        if (ward != null && !ward.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(ward);
        }
        if (district != null && !district.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(district);
        }
        if (province != null && !province.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(province);
        }
        return fullAddress.toString().isEmpty() ? "Chưa cập nhật" : fullAddress.toString();
    }
}
