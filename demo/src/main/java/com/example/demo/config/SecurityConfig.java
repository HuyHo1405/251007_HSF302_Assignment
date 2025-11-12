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

    private static final String[] PUBLIC_URLS = {
            "/auth**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/img/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        // Public access
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // Home - accessible to all authenticated users
                        .requestMatchers("/", "/home").authenticated()

                        // Product management
                        .requestMatchers("/products/create", "/products/update/**", "/products/delete/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/products/**").authenticated()

                        // Cart - Tất cả user đều được mua hàng
                        .requestMatchers("/cart/**").authenticated()

                        // Order management
                        .requestMatchers("/orders/statistics").hasRole("ADMIN")
                        .requestMatchers("/orders/{id}/status").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/orders/{id}/delete").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/orders/{id}/items/{itemId}/delete").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/orders/{id}/items/{itemId}/edit").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/orders/{id}/items/add").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/orders/{id}/cancel").authenticated()
                        .requestMatchers("/orders/**").authenticated()

                        // User management
                        .requestMatchers("/users/profile", "/users/profile/edit").authenticated()
                        .requestMatchers("/users/list").hasAnyRole("ADMIN", "STAFF") // SỬA: STAFF được xem danh sách
                        .requestMatchers("/users/{id}").hasAnyRole("ADMIN", "STAFF") // SỬA: STAFF được xem profile user khác
                        .requestMatchers("/users/{id}/delete").hasRole("ADMIN") // CHỈ ADMIN xóa user
                        .requestMatchers("/users/**").authenticated()

                        // Payment
                        .requestMatchers("/payments/**").authenticated()

                        // Default
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
                        .accessDeniedPage("/access-denied")
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