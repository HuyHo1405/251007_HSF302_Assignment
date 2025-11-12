package com.example.demo.service;

import com.example.demo.config.VnpayConfig;
import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.Payment;
import com.example.demo.model.dto.PaymentDTO;
import com.example.demo.repo.OrderRepository;
import com.example.demo.repo.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;
    private final VnpayConfig vnpayConfig;
    private final HttpServletRequest request;

    @Override
    @Transactional
    public String createVnpayPayment(Long orderId) {
        // 1. Tìm order
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 2. Tạo orderCode unique
        String orderCode = "VNP" + System.currentTimeMillis();

        // 3. Tạo Payment entity với status PENDING
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod("vnpay")
                .amount(order.getTotalPrice())
                .status("pending")
                .orderCode(orderCode)
                .build();
        paymentRepo.save(payment);

        // 4. Chuẩn bị params cho VNPay
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", VnpayConfig.VERSION);
        vnpParams.put("vnp_Command", VnpayConfig.COMMAND_PAY);
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf((long)(order.getTotalPrice() * 100))); // x100
        vnpParams.put("vnp_CurrCode", VnpayConfig.CURR_CODE);
        vnpParams.put("vnp_TxnRef", orderCode);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", getIpAddress());

        String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        vnpParams.put("vnp_CreateDate", createDate);

        // 5. Tạo query string
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                 .append("=")
                 .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                 .append("&");
        }
        String queryString = query.substring(0, query.length() - 1);

        // 6. Tạo secure hash
        String hashData = queryString;
        String vnpSecureHash = createVnpayHash(hashData, vnpayConfig.getHashSecret());

        // 7. Tạo payment URL
        String paymentUrl = vnpayConfig.getUrl() + "?" + queryString + "&vnp_SecureHash=" + vnpSecureHash;

        return paymentUrl;
    }

    @Override
    @Transactional
    public Map<String, String> handleVnpayReturn(Map<String, String> vnpayParams) {
        Map<String, String> result = new HashMap<>();

        try {
            // 1. Lấy và remove secure hash
            String receivedHash = vnpayParams.get("vnp_SecureHash");
            vnpayParams.remove("vnp_SecureHash");
            vnpayParams.remove("vnp_SecureHashType");

            // 2. Sắp xếp params và tạo hash
            Map<String, String> sortedParams = new TreeMap<>(vnpayParams);
            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                       .append("=")
                       .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                       .append("&");
            }
            String data = hashData.substring(0, hashData.length() - 1);
            String calculatedHash = createVnpayHash(data, vnpayConfig.getHashSecret());

            // 3. Verify signature
            if (!calculatedHash.equals(receivedHash)) {
                result.put("status", "error");
                result.put("message", "Invalid signature");
                return result;
            }

            // 4. Lấy thông tin
            String orderCode = vnpayParams.get("vnp_TxnRef");
            String responseCode = vnpayParams.get("vnp_ResponseCode");
            String transactionNo = vnpayParams.get("vnp_TransactionNo");

            // 5. Tìm payment
            Payment payment = paymentRepo.findByOrderCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            // 6. Lưu response data
            payment.setResponseCode(responseCode);
            payment.setTransactionRef(transactionNo);
            payment.setRawResponseData(vnpayParams.toString());

            // 7. Xử lý theo response code
            if ("00".equals(responseCode)) {
                payment.setStatus("successful");
                payment.setPaidAt(LocalDateTime.now());

                // Cập nhật order status
                Order order = payment.getOrder();
                order.setStatus("paid");
                orderRepo.save(order);

                result.put("status", "success");
                result.put("message", "Thanh toán thành công");
            } else {
                payment.setStatus("failed");
                result.put("status", "failed");
                result.put("message", "Thanh toán thất bại (Mã: " + responseCode + ")");
            }

            paymentRepo.save(payment);
            result.put("orderCode", orderCode);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    @Override
    public Payment getPaymentByOrderCode(String orderCode) {
        return paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @Override
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepo.findByOrderId(orderId);
    }

    @Override
    public List<PaymentDTO> getPaymentDTOsByOrderId(Long orderId) {
        return paymentRepo.findByOrderId(orderId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Payment createCODPayment(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod("cash")
                .amount(order.getTotalPrice())
                .status("pending")
                .orderCode("COD" + System.currentTimeMillis())
                .build();

        return paymentRepo.save(payment);
    }

    @Override
    @Transactional
    public Map<String, String> simulateVnpaySuccess(String orderCode) {
        // 1. Tìm payment
        Payment payment = paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 2. Kiểm tra status
        if (!"pending".equals(payment.getStatus())) {
            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "Payment already processed");
            return result;
        }

        // 3. Tạo fake VNPay params giống như VNPay trả về
        Map<String, String> fakeParams = new TreeMap<>();
        fakeParams.put("vnp_Amount", String.valueOf((long)(payment.getAmount() * 100)));
        fakeParams.put("vnp_BankCode", "NCB");
        fakeParams.put("vnp_BankTranNo", "VNPAY" + System.currentTimeMillis());
        fakeParams.put("vnp_CardType", "ATM");
        fakeParams.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
        fakeParams.put("vnp_PayDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        fakeParams.put("vnp_ResponseCode", "00"); // 00 = success
        fakeParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        fakeParams.put("vnp_TransactionNo", String.valueOf(System.currentTimeMillis()));
        fakeParams.put("vnp_TransactionStatus", "00");
        fakeParams.put("vnp_TxnRef", orderCode);

        // 4. Tạo hash hợp lệ
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : fakeParams.entrySet()) {
            hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                   .append("=")
                   .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                   .append("&");
        }
        String data = hashData.substring(0, hashData.length() - 1);
        String secureHash = createVnpayHash(data, vnpayConfig.getHashSecret());
        fakeParams.put("vnp_SecureHash", secureHash);

        // 5. Gọi handleVnpayReturn với fake params
        return handleVnpayReturn(fakeParams);
    }

    /**
     * REFUND VNPAY PAYMENT
     * Gọi VNPay API để hoàn tiền cho giao dịch đã thanh toán
     *
     * VNPay Refund API: https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
     * Method: POST
     *
     * @param orderId ID của order cần refund
     * @param reason Lý do refund
     * @return Map chứa status và message
     */
    @Override
    @Transactional
    public Map<String, String> refundVnpayPayment(Long orderId, String reason) {
        Map<String, String> result = new HashMap<>();

        try {
            // 1. Tìm order
            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // 2. Tìm payment đã thanh toán thành công
            List<Payment> payments = paymentRepo.findByOrderId(orderId);
            Payment successfulPayment = payments.stream()
                    .filter(p -> "successful".equals(p.getStatus()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No successful payment found for this order"));

            // 3. Kiểm tra đã refund chưa (check transactionRef thay vì responseCode)
            if (successfulPayment.getTransactionRef() != null &&
                successfulPayment.getTransactionRef().startsWith("REFUND")) {
                result.put("status", "error");
                result.put("message", "Payment đã được refund trước đó");
                return result;
            }

            // 4. Tạo refund request params cho VNPay API
            String refundCode = "REFUND" + System.currentTimeMillis();
            String transDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            Map<String, String> refundParams = new TreeMap<>();
            refundParams.put("vnp_RequestId", refundCode); // Mã yêu cầu refund unique
            refundParams.put("vnp_Version", VnpayConfig.VERSION);
            refundParams.put("vnp_Command", "refund"); // Command refund
            refundParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
            refundParams.put("vnp_TransactionType", "02"); // 02 = toàn bộ, 03 = một phần
            refundParams.put("vnp_TxnRef", successfulPayment.getOrderCode()); // Mã giao dịch gốc
            refundParams.put("vnp_Amount", String.valueOf((long)(successfulPayment.getAmount() * 100))); // Số tiền refund
            refundParams.put("vnp_OrderInfo", "Hoan tien don hang " + orderId + ". Ly do: " + reason);
            refundParams.put("vnp_TransactionNo", successfulPayment.getTransactionRef()); // Mã giao dịch VNPay gốc
            refundParams.put("vnp_TransactionDate", transDate);
            refundParams.put("vnp_CreateBy", "admin"); // User thực hiện refund
            refundParams.put("vnp_CreateDate", transDate);
            refundParams.put("vnp_IpAddr", getIpAddress());

            // 5. Tạo secure hash cho refund request
            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : refundParams.entrySet()) {
                hashData.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            String data = hashData.substring(0, hashData.length() - 1);
            String secureHash = createVnpayHash(data, vnpayConfig.getHashSecret());
            refundParams.put("vnp_SecureHash", secureHash);

            // 6. GỌI VNPAY API (Trong production, cần gọi HTTP POST)
            // URL: https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
            // Hiện tại MOCK để test (vì VNPay sandbox cần merchant account thật)

            System.out.println("=== VNPAY REFUND REQUEST ===");
            System.out.println("Refund Code: " + refundCode);
            System.out.println("Order ID: " + orderId);
            System.out.println("Amount: " + successfulPayment.getAmount());
            System.out.println("Original Transaction: " + successfulPayment.getTransactionRef());
            System.out.println("Reason: " + reason);
            System.out.println("===========================");

            // 7. Cập nhật payment record
            String originalTransactionRef = successfulPayment.getTransactionRef();

            // UPDATE STATUS: successful → refunded
            successfulPayment.setStatus("refunded");
            successfulPayment.setTransactionRef(refundCode); // Lưu refund code

            // Lưu thông tin refund vào rawResponseData
            String refundInfo = String.format(
                "[REFUND] %s | Code: %s | Amount: %.0f | Original Txn: %s | Reason: %s",
                LocalDateTime.now(),
                refundCode,
                successfulPayment.getAmount(),
                originalTransactionRef,
                reason
            );

            String existingData = successfulPayment.getRawResponseData() != null ?
                successfulPayment.getRawResponseData() : "";
            successfulPayment.setRawResponseData(existingData + "\n" + refundInfo);

            paymentRepo.save(successfulPayment);

            // 8. Log thành công (trong thực tế sẽ parse response từ VNPay)
            result.put("status", "success");
            result.put("message", "Đã tạo yêu cầu hoàn tiền thành công");
            result.put("refundCode", refundCode);
            result.put("amount", String.valueOf(successfulPayment.getAmount()));

            System.out.println("=== REFUND SUCCESS ===");
            System.out.println("Refund Code: " + refundCode);
            System.out.println("Amount: " + successfulPayment.getAmount() + " VND");
            System.out.println("======================");

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            System.err.println("=== REFUND ERROR ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("====================");
        }

        return result;
    }

    // Helper: Tạo HMAC SHA512 hash
    private String createVnpayHash(String data, String secret) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSha512.init(secretKey);
            byte[] hash = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating hash", e);
        }
    }

    // Helper: Lấy IP address
    private String getIpAddress() {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "127.0.0.1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }

    // Helper: Convert Payment entity to DTO
    private PaymentDTO toDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderCode(payment.getOrderCode())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .responseCode(payment.getResponseCode())
                .paidAt(payment.getPaidAt())
                .build();
    }

}
