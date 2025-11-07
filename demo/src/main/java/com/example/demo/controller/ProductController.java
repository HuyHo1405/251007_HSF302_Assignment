package com.example.demo.controller;


import com.example.demo.model.dto.ProductDetailDTO;
import com.example.demo.model.dto.ProductListDTO;
import com.example.demo.model.entity.Product;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    //List sản phẩm với filter, paginate, sort
    @GetMapping("/list")
    public String getProduct(@RequestParam (required = false, defaultValue = "1") int pageNo,
                                           @RequestParam (required = false, defaultValue = "5") int pageSize,
                                           @RequestParam (required = false, defaultValue = "id") String sortBy,
                                           @RequestParam (required = false, defaultValue = "DESC") String sortDir,
                                           @RequestParam (required = false) String name,
                                           @RequestParam (required = false) String brand,
                                           @RequestParam (required = false) Double unitPrice,
                                           Model model) {


        Sort sort = null;
        if(sortDir.equalsIgnoreCase("DESC")){
            sort = Sort.by(sortBy).ascending();
        }else{
            sort = Sort.by(sortBy).descending();
        }
        //set attribute to view
        List<ProductListDTO> productsList = productService.getProductList(PageRequest.of(pageNo-1, pageSize), name, brand, unitPrice);

        model.addAttribute("productsList", productsList);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("DESC") ? "ASC" : "DESC");
        model.addAttribute("name", name);
        model.addAttribute("brand", brand);
        model.addAttribute("unitPrice", unitPrice);
        return "productlist";
    }

    //Xem chi tiết sản phẩm
    @GetMapping("/detail/{id}")
    public String getProductDetail(@PathVariable Long id, Model model){
        ProductDetailDTO proDetails = productService.getProductDetail(id);
        model.addAttribute("productdetail", proDetails);
        return "productdetail";
    }

    //Form tạo sản phẩm
    @GetMapping("/create")
    public String createProductForm(Model model){
        model.addAttribute("product", new Product());
        return "productcreate";
    }
    //Tạo sản phẩm
    @PostMapping("/create")
    public String createProduct(@ModelAttribute("product") Product pro, Model model){
        productService.createProduct(pro);
        return "redirect:/products/list";
    }
    //update form
    @GetMapping("/update/{id}")
    public String updateProductForm(Model model, @PathVariable Long id){
        model.addAttribute("product", productService.getProductById(id));
        return "productupdate";
    }
    //Sửa sản phẩm
    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable Long id,  @ModelAttribute("product") Product pro){
        pro.setId(id);
        productService.updateProduct(pro, id);
        return "redirect:/products/list";
    }

    @GetMapping("/delete/{id}")
    //Xóa sản phẩm
    public String deleteProduct(@PathVariable Long id){
        productService.deleteProduct(id);
        return "redirect:/products/list";
    }
}
