package com.example.demo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VnpayConfig {
    // Lấy từ email
    @Value("${vnpay.tmnCode}")
    private String tmnCode; // = 4BX9X4TI

    @Value("${vnpay.hashSecret}")
    private String hashSecret; // = Y702OFN14NI1022B4EBLUX574MY3X4LK

    // Lấy từ email
    @Value("${vnpay.url}")
    private String url; // = https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

    // Đây là URL mà VNPAY sẽ trả người dùng về sau khi thanh toán
    @Value("${vnpay.returnUrl}")
    private String returnUrl; // Ví dụ: http://localhost:8080/api/v1/payments/vnpay-callback

    // Đây là URL mà VNPAY sẽ gọi "ngầm" (IPN)
    @Value("${vnpay.ipnUrl}")
    private String ipnUrl; // Ví dụ: http://localhost:8080/api/payments/vnpay-ipn

    // Các hằng số khác
    public static final String VERSION = "2.1.0";
    public static final String COMMAND_PAY = "pay";
    public static final String CURR_CODE = "VND";
}
