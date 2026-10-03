package com.cp.friend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers(
                                                                "/",
                                                                "/login",
                                                                "/home",
                                                                "/register", 
                                                                "/room",
                                                                "/setting",
                                                                "/notification")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/api/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/favicon.ico")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable());

                return http.build();
        }

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .servers(List.of(
                                                new Server().url("/")))
                                .info(new Info()
                                                .title("Friend API")
                                                .version("1.0.0")
                                                .description("REST API for Friend Application")
                                                .contact(
                                                                new Contact().name("CP Friend")));
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
