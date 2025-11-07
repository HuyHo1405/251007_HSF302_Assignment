package fa25.studentcode.demoproduct.model.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProductListDTO {
    private Long id;

    private String name;

    private String brand;

    private String model;

    private Double unitPrice;

    private String status;

    private String imageUrl;
}
