package com.example.demo.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    private Long id;           // id của dòng item (để xóa/sửa)
    private Long orderId;      // id đơn chứa item này
    private Long productId;    // id sản phẩm
    private String productName;// tên sản phẩm (show detail)
    private Integer quantity;  // số lượng mua
    private Double unitPrice;  // đơn giá
    private Double subtotal;   // tiền cho dòng này
}

