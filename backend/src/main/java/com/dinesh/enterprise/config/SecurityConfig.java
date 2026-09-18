package com.dinesh.enterprise.config;

import com.dinesh.enterprise.security.JwtAuthenticationEntryPoint;
import com.dinesh.enterprise.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    @Value("${FRONTEND_URL:${app.frontend-url:http://localhost:5173}}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Public read-only catalog endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                // Category write operations — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")

                // Product write operations — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                // Warehouse management
                .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "SUPPORT_AGENT")
                .requestMatchers("/api/v1/warehouses/**").hasRole("ADMIN")

                // Inventory management
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "SUPPORT_AGENT")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                // Analytics & Reporting
                .requestMatchers("/api/v1/analytics/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "SUPPORT_AGENT")

                // Order management
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/my-orders").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "SUPPORT_AGENT")
                .requestMatchers("/api/v1/orders/**").authenticated()

                // User management — ADMIN only (except /me which is authenticated)
                .requestMatchers("/api/v1/users/me").authenticated()
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                // Test endpoints for role verification
                .requestMatchers("/api/v1/test/customer").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/test/admin").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new ArrayList<>(List.of("http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"));
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            for (String url : frontendUrl.split(",")) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
