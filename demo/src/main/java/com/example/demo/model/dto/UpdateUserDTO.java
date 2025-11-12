package com.example.demo.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateUserDTO {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String emailAddress;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố")
    private String province;

    @NotBlank(message = "Vui lòng chọn Quận/Huyện")
    private String district;

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;
}
