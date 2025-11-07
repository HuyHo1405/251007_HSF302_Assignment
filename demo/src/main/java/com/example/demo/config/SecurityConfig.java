package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        // Public access
                        .requestMatchers("/auth/register", "/auth/login", "/css/**", "/js/**", "/images/**", "/img/**").permitAll()

                        // Home - accessible to all authenticated users
                        .requestMatchers("/", "/home").authenticated()

                        // Product management - ADMIN & STAFF only can create/update/delete
                        .requestMatchers("/products/create", "/products/update/**", "/products/delete/**").hasAnyRole("ADMIN", "STAFF")
                        // Product list & detail - accessible to all authenticated users
                        .requestMatchers("/products/**").authenticated()

                        // Cart - accessible to all authenticated users (mainly CUSTOMER)
                        .requestMatchers("/cart/**").authenticated()

                        // Order management
                        .requestMatchers("/orders").authenticated() // Tất cả user đã login đều truy cập được, tự động lọc theo role
                        .requestMatchers("/orders/statistics").hasRole("ADMIN") // Chỉ Admin xem thống kê
                        .requestMatchers("/orders/*/status", "/orders/*/delete").hasAnyRole("ADMIN", "STAFF") // Staff/Admin quản lý đơn
                        .requestMatchers("/orders/**").authenticated() // Các endpoint khác cần đăng nhập

                        // User management
                        .requestMatchers("/users/profile", "/users/profile/edit", "/users/dashboard").authenticated() // Tất cả user xem/sửa profile
                        .requestMatchers("/users/list", "/users/admin/**").hasAnyRole("ADMIN", "STAFF") // Chỉ Admin/Staff xem danh sách user
                        .requestMatchers("/users/**").authenticated()

                        // Payment
                        .requestMatchers("/payments/**").authenticated()

                        // Default - require authentication
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("phoneNumber")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout((logout) -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling((exception) -> exception
                        .accessDeniedPage("/access-denied") // Trang báo lỗi khi không có quyền
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(passwordEncoder);
        authProvider.setUserDetailsService(userDetailsService);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
