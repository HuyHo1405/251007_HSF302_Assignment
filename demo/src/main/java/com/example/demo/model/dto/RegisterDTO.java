package com.example.demo.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDTO {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 3, message = "Họ tên phải có ít nhất 3 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String emailAddress;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09)")
    private String phoneNumber;

    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố")
    private String province;

    @NotBlank(message = "Vui lòng chọn Quận/Huyện")
    private String district;

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
}