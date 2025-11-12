package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email_address", unique = true)
    private String emailAddress;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "province")
    private String province;  // Tỉnh/Thành phố

    @Column(name = "district")
    private String district;  // Quận/Huyện

    @Column(name = "ward")
    private String ward;  // Phường/Xã

    @Column(name = "address_detail")
    private String addressDetail;  // Địa chỉ chi tiết

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if(role == null){
            role = Role.CUSTOMER;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return phoneNumber;
    }

    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        if (addressDetail != null && !addressDetail.isEmpty()) {
            fullAddress.append(addressDetail);
        }
        if (ward != null && !ward.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(ward);
        }
        if (district != null && !district.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(district);
        }
        if (province != null && !province.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(province);
        }
        return fullAddress.toString().isEmpty() ? "Chưa cập nhật" : fullAddress.toString();
    }

    public enum  Role {
        CUSTOMER,
        STAFF,
        ADMIN
    }
}
