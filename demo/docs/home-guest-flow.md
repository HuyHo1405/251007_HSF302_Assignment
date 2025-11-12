# Thiết kế lại trang Home & bảo vệ quyền sở hữu đơn hàng

## 1. Tổng quan luồng người dùng

- **Khách (guest)** có thể truy cập `"/"` và `"/home"` để xem top 10 sản phẩm bán chạy cùng các thông tin giới thiệu.
- **Bất kỳ hành động giao dịch** (truy cập giỏ hàng, thêm sản phẩm vào giỏ, xem đơn hàng, thống kê, v.v.) đều yêu cầu đăng nhập. Khi khách bấm nút mua sẽ được dẫn thẳng đến `/auth/login`.
- **Người dùng đã đăng nhập** giữ nguyên toàn bộ menu điều hướng. Từ trang home họ có thể thêm nhanh sản phẩm best seller vào giỏ.
- **Bảo vệ quyền sở hữu đơn hàng**: nếu người dùng (kể cả khi đã đăng nhập bằng tài khoản khác) cố mở đường dẫn `/orders/{id}` không thuộc về mình thì hiển thị trang cảnh báo “Đây không phải đơn hàng của bạn” và cung cấp nút quay về danh sách đơn.

## 2. Thay đổi chi tiết phía backend

### 2.1. Bảo mật (Spring Security)
- `SecurityConfig` mở quyền truy cập `"/"` và `"/home"` cho tất cả, giữ nguyên yêu cầu đăng nhập với các route còn lại.
- Các endpoint `/cart/**`, `/orders/**`, `/users/**`… vẫn yêu cầu xác thực nên mọi thao tác của khách sẽ bị Spring Security chuyển hướng về `/auth/login`.

```28:48:demo/src/main/java/com/example/demo/config/SecurityConfig.java
.requestMatchers("/", "/home").permitAll()
```

### 2.2. Lấy dữ liệu Top 10 sản phẩm
- Tạo DTO `BestSellerProductDTO` chứa thông tin cần hiển thị (id, tên, thương hiệu, giá, tổng số đã bán, ảnh).
- Bổ sung quan hệ `Product.orderItems` để JPA join ngược từ sản phẩm sang chi tiết đơn hàng.
- `ProductRepository` có query đếm tổng số lượng đã giao (`status = 'delivered'`) và trả về danh sách theo `Pageable`.
- `ProductService#getTopBestSellers(int limit)` sử dụng repository mới để lấy 10 sản phẩm.

```1:34:demo/src/main/java/com/example/demo/model/dto/BestSellerProductDTO.java
// ... existing code ...
```

```13:35:demo/src/main/java/com/example/demo/repo/ProductRepository.java
@Query("""
    SELECT new com.example.demo.model.dto.BestSellerProductDTO(
        p.id,
        p.name,
        p.brand,
        p.unitPrice,
        COALESCE(SUM(CASE WHEN o.status = 'delivered' THEN oi.quantity ELSE 0 END), 0),
        COALESCE(MIN(pi.url), '')
    )
    FROM Product p
    LEFT JOIN p.productImages pi
    LEFT JOIN p.orderItems oi
    LEFT JOIN oi.order o
    GROUP BY p.id, p.name, p.brand, p.unitPrice
    ORDER BY COALESCE(SUM(CASE WHEN o.status = 'delivered' THEN oi.quantity ELSE 0 END), 0) DESC
    """)
List<BestSellerProductDTO> findTopBestSellers(Pageable pageable);
```

```19:37:demo/src/main/java/com/example/demo/service/ProductService.java
public List<BestSellerProductDTO> getTopBestSellers(int limit) {
    return productRepo.findTopBestSellers(PageRequest.of(0, limit));
}
```

### 2.3. HomeController
- Tiêm `ProductService`, kiểm tra người dùng có đăng nhập không.
- Chỉ khởi tạo giỏ hàng khi người dùng đã đăng nhập.
- Truyền `topProducts` và trạng thái xác thực vào view.

```19:34:demo/src/main/java/com/example/demo/controller/HomeController.java
boolean isAuthenticated = authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
// ...
model.addAttribute("topProducts", productService.getTopBestSellers(10));
```

### 2.4. OrderController
- Nếu người dùng hiện tại là **CUSTOMER** nhưng đơn không thuộc về họ, render template `orders/not-owner.html` thay vì redirect kèm flash message.

```98:105:demo/src/main/java/com/example/demo/controller/OrderController.java
if (currentUser.getRole() == User.Role.CUSTOMER && !dto.getUserId().equals(currentUser.getId())) {
    model.addAttribute("orderId", id);
    return "orders/not-owner";
}
```

## 3. Thay đổi giao diện

### 3.1. Home (`templates/home.html`)
- Thiết kế lại hoàn toàn: nền động chủ đề laptop, hero section, grid hiển thị top 10 sản phẩm.
- Nút “Thêm vào giỏ” chỉ hiển thị khi đã đăng nhập; khách thấy nút “Đăng nhập để mua”.
- Các nút form có kèm CSRF token.

```1:210:demo/src/main/resources/templates/home.html
<!-- xem file để biết full markup & style -->
```

### 3.2. Thông báo không có quyền xem đơn
- Trang `templates/orders/not-owner.html` hiển thị thông điệp và nút quay về `/orders/my`.

```1:52:demo/src/main/resources/templates/orders/not-owner.html
<!-- xem file để biết chi tiết -->
```

## 4. Kiểm thử khuyến nghị

1. **Guest**
   - Truy cập `/home`: thấy hero + top 10 + nút đăng nhập.
   - Click “Đăng nhập để mua” phải chuyển tới `/auth/login`.
   - Truy cập trực tiếp `/cart` hoặc `/orders` → Spring Security chuyển sang `/auth/login`.
2. **Người dùng đã đăng nhập**
   - Truy cập `/home`: thấy nút “Thêm vào giỏ” hoạt động, sản phẩm vào giỏ thành công.
   - Kiểm tra top 10 khớp dữ liệu thực tế (đơn `delivered`).
3. **Bảo vệ đơn hàng**
   - Dùng tài khoản A tạo đơn, đăng xuất.
   - Đăng nhập tài khoản B, truy cập `/orders/{id}` của A → nhận trang cảnh báo.
   - Đăng nhập bằng tài khoản ADMIN/STAFF vẫn xem được đơn để đảm bảo nghiệp vụ quản trị.

Sau khi hoàn thành chỉnh sửa, nên chạy `mvn clean test` để đảm bảo các thành phần hoạt động ổn định.




