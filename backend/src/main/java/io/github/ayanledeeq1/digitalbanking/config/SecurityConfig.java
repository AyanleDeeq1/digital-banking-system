package io.github.ayanledeeq1.digitalbanking.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origin}")
    private  String allowedOrigin;

    @Bean 
    public  PasswordEncoder passwordEncoder() {
        return  new BCryptPasswordEncoder();
    }

    @Bean 
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        System.out.println(allowedOrigin);
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of(allowedOrigin)
        );
        configuration.setAllowCredentials(true);

        configuration.setAllowedMethods(
              List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
             List.of("Authorization", "Content-Type", "X-XSRF-TOKEN")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);
        return  source;
    }

    @Bean
    public  SecurityFilterChain securityFilterChain(HttpSecurity http, UrlBasedCorsConfigurationSource configurationSource) throws Exception {

        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf
                            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
            .cors(cors -> cors
                            .configurationSource(configurationSource))
            .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST,  "/api/customers", "/api/customers/login").permitAll()
            .requestMatchers(HttpMethod.GET,  "/api/customers/csrf").permitAll()
            .requestMatchers(HttpMethod.GET,  "/api/customers/me").authenticated()
            .requestMatchers(HttpMethod.GET,  "/api/customers/accounts").authenticated()
            .requestMatchers(HttpMethod.POST,  "/api/customers", "/api/customers/createAccount").authenticated()
            .anyRequest().authenticated()
            );
        
        return http.build();
        
    }

    @Bean 
    public  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return  configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return  new HttpSessionSecurityContextRepository();
    }

}
