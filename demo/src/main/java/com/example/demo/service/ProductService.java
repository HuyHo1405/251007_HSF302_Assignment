package com.example.demo.service;


import com.example.demo.model.dto.BestSellerProductDTO;
import com.example.demo.model.dto.ProductDetailDTO;
import com.example.demo.model.dto.ProductListDTO;
import com.example.demo.model.entity.Product;
import com.example.demo.model.mapper.ProductMapper;
import com.example.demo.model.specification.ProductSpecification;
import com.example.demo.repo.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepo;

    //Xem tất cả sản phẩm, filter, paginate
    public List<ProductListDTO> getProductList(Pageable pageable, String name, String brand, Double unitPrice){
        Specification<Product> spec = ProductSpecification.searchBy(name, brand, unitPrice);
         return productRepo.findAll(spec, pageable).map(ProductMapper::toListDTO).getContent();
    }

    public List<BestSellerProductDTO> getTopBestSellers(int limit) {
        return productRepo.findTopBestSellers(PageRequest.of(0, limit));
    }

    //Xem chi tiết sản phẩm
    public ProductDetailDTO getProductDetail(Long id){
        return productRepo.findById(id).map(ProductMapper::toDetailDTO).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found" + id));
    }

    //Thêm sản phẩm
    public void createProduct(Product pro){
        productRepo.save(pro);
    }
    //Get product by id
    public Product getProductById(Long id){
        return productRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found" + id));
    }
    //Sửa
    public void updateProduct(Product pro, Long id){
        Product productExist = productRepo.findById(id).orElse(null);
        if(productExist != null){
            productExist.setId(pro.getId());
            productExist.setName(pro.getName());
            productExist.setSku(pro.getSku());
            productExist.setType(pro.getType());
            productExist.setBrand(pro.getBrand());
            productExist.setModel(pro.getModel());
            productExist.setUnitPrice(pro.getUnitPrice());
            productExist.setDescription(pro.getDescription());
            productExist.setStatus(pro.getStatus());
            productExist.setWarrantyMonths(pro.getWarrantyMonths());
            productExist.setCreatedAt(pro.getCreatedAt());
        }
        productRepo.save(productExist);
    }

    //Xóa
    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }

    // ===== STOCK MANAGEMENT METHODS (Simplified - No Reserved Logic) =====

    /**
     * Decrease stock khi giao hàng (delivered)
     * Trừ trực tiếp từ stockQuantity
     */
    public void decreaseStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0");
        }

        Product product = getProductById(productId);

        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (currentStock < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không đủ hàng trong kho. Còn: " + currentStock + ", Yêu cầu: " + quantity);
        }

        product.setStockQuantity(currentStock - quantity);
        productRepo.save(product);
    }

    /**
     * Restore stock khi hủy đơn hàng
     * Cộng lại vào stockQuantity
     */
    public void restoreStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0");
        }

        Product product = getProductById(productId);
        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        product.setStockQuantity(currentStock + quantity);
        productRepo.save(product);
    }

    /**
     * Check stock availability (chỉ check stockQuantity)
     */
    public boolean isStockAvailable(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        Product product = getProductById(productId);
        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        return currentStock >= quantity;
    }

}
