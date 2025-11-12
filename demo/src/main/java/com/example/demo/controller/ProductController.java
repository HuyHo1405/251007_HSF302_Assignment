package com.example.demo.controller;


import com.example.demo.model.dto.ProductDetailDTO;
import com.example.demo.model.dto.ProductListDTO;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.User;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    //List sản phẩm với filter, paginate, sort
    @GetMapping("/list")
    public String getProduct(@RequestParam (required = false, defaultValue = "1") int pageNo,
                                           @RequestParam (required = false, defaultValue = "8") int pageSize,
                                           @RequestParam (required = false, defaultValue = "id") String sortBy,
                                           @RequestParam (required = false, defaultValue = "DESC") String sortDir,
                                           @RequestParam (required = false) String name,
                                           @RequestParam (required = false) String brand,
                                           @RequestParam (required = false) Double unitPrice,
                                           @RequestParam(required = false) String type,
                                           @AuthenticationPrincipal User currentUser,
                                           Model model) {

        Sort sort = null;
        if(sortDir.equalsIgnoreCase("DESC")){
            sort = Sort.by(sortBy).ascending();
        }else{
            sort = Sort.by(sortBy).descending();
        }
        //set attribute to view
        List<ProductListDTO> productsList = productService.getProductList(PageRequest.of(pageNo-1, pageSize), name, brand, unitPrice, type);


        model.addAttribute("productsList", productsList);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("DESC") ? "ASC" : "DESC");
        model.addAttribute("name", name);
        model.addAttribute("brand", brand);
        model.addAttribute("type", type);
        model.addAttribute("unitPrice", unitPrice);
        model.addAttribute("currentUser", currentUser); // Truyền currentUser để check role trong view
        return "products/productlist";
    }

    //Xem chi tiết sản phẩm
    @GetMapping("/detail/{id}")
    public String getProductDetail(@PathVariable Long id,
                                   @AuthenticationPrincipal User currentUser,
                                   Model model){
        ProductDetailDTO proDetails = productService.getProductDetail(id);
        model.addAttribute("productdetail", proDetails);
        model.addAttribute("currentUser", currentUser);
        return "products/productdetail";
    }

    //Form tạo sản phẩm
    @GetMapping("/create")
    public String createProductForm(@AuthenticationPrincipal User currentUser, Model model){
        model.addAttribute("product", new Product());
        model.addAttribute("currentUser", currentUser);
        return "products/productcreate";
    }

    //Tạo sản phẩm
    @PostMapping("/create")
    public String createProduct(@ModelAttribute("product") Product pro,
                               RedirectAttributes redirectAttributes,
                                @RequestParam("images") List<MultipartFile> multipartFile){
        try {
            productService.createProduct(pro, multipartFile);
            redirectAttributes.addFlashAttribute("success", "Tạo sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/products/list";
    }

    //update form
    @GetMapping("/update/{id}")
    public String updateProductForm(Model model, @PathVariable Long id,
                                   @AuthenticationPrincipal User currentUser){
        model.addAttribute("product", productService.getProductById(id));
        model.addAttribute("currentUser", currentUser);
        return "products/productupdate";
    }

    //Sửa sản phẩm
    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable Long id,
                               @ModelAttribute("product") Product pro,
                               RedirectAttributes redirectAttributes){
        try {
            pro.setId(id);
            productService.updateProduct(pro, id);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/products/list";
    }

    @GetMapping("/delete/{id}")
    //Xóa sản phẩm
    public String deleteProduct(@PathVariable Long id,
                               RedirectAttributes redirectAttributes){
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/products/list";
    }
}
