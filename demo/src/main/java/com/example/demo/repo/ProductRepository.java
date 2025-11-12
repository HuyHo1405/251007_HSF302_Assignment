package com.example.demo.repo;

import com.example.demo.model.dto.BestSellerProductDTO;
import com.example.demo.model.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    // Có thể thêm các method custom sau này nếu cần filter

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
    java.util.List<BestSellerProductDTO> findTopBestSellers(Pageable pageable);
}