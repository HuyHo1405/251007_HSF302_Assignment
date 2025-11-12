package com.example.demo.model.specification;

import com.example.demo.model.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {
    public static Specification<Product> searchBy(String name, String brand, Double unitPrice, String type){

        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("name")),
                        "%" + name.toLowerCase().trim() + "%"));
            }
            if (brand != null && !brand.isBlank()) {
                // nếu muốn match chính xác thì dùng equal + toLowerCase
                ps.add(cb.like(cb.lower(root.get("brand")),
                        "%" + brand.toLowerCase().trim() + "%"));
            }
            if (unitPrice != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("unitPrice"), unitPrice));
            }
            if (type != null && !type.isBlank()) {
                ps.add(cb.equal(cb.lower(root.get("type")),
                        type.toLowerCase().trim())); // LAPTOP / PART
            }

            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));
        };
    }
       /* return new Specification<Product>() {
            @Override
            //Predicate = condition query in database
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

                //create list to add condition query(criteriaBuider->provide method to create condition query)
                List<Predicate> list = new ArrayList<>();

                //logic condition query
                if(name!=null && !name.isEmpty()){
                    list.add(criteriaBuilder.like(root.get("name"),"%" + name + "%"));
                }
                if(brand!=null && !brand.isEmpty()){
                    list.add(criteriaBuilder.equal(root.get("brand"),brand));
                }
                if(unitPrice!=null){
                    list.add(criteriaBuilder.lessThanOrEqualTo(root.get("unitPrice"),  unitPrice ));
                }
                if (type != null && !type.isBlank()) { // <--- new

                }
                if(list.isEmpty()){
                    return criteriaBuilder.conjunction();
                }
                return criteriaBuilder.or(list.toArray(new Predicate[0]));
            }
        };
    }*/
}
