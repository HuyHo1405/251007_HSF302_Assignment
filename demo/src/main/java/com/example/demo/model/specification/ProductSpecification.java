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
    public static Specification<Product> searchBy(String name, String brand, Double unitPrice){
        return new Specification<Product>() {
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
                    list.add(criteriaBuilder.equal(root.get("unitPrice"),  unitPrice ));
                }
                if(list.isEmpty()){
                    return criteriaBuilder.conjunction();
                }
                return criteriaBuilder.or(list.toArray(new Predicate[0]));
            }
        };
    }
}
