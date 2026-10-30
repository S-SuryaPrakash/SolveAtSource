package com.example.resolveX.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary security bootstrap configuration.
 *
 * <p>Adding Spring Security otherwise protects every route with its generated
 * default login mechanism. This keeps the existing API behaviour unchanged while
 * JWT authentication is introduced in later phases. It will be replaced with
 * stateless JWT validation and explicit authorization rules.</p>
 */
@Configuration
@EnableConfigurationProperties({BootstrapAdminProperties.class, JwtProperties.class})
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
