package com.cp.friend.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cp.friend.controller.AuthController;
import com.cp.friend.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @MockitoBean
    private AuthService authService;

    @Test
    void publicPages_areNotBlockedBySecurity() throws Exception {
        String[] paths = {
                "/",
                "/login",
                "/home",
                "/register"
        };

        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status != 401 && status != 403);
                    });
        }
    }

    @Test
    void api_isAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk());
    }

    @Test
    void apiPost_doesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void swaggerRoutes_areNotBlockedBySecurity() throws Exception {
        String[] paths = {
                "/swagger-ui/index.html",
                "/v3/api-docs/openapi"
        };

        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status != 401 && status != 403);
                    });
        }
    }

    @Test
    void staticResources_areNotBlockedBySecurity() throws Exception {
        String[] paths = {
                "/css/app.css",
                "/js/app.js",
                "/images/logo.png",
                "/favicon.ico"
        };

        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status != 401 && status != 403);
                    });
        }
    }

    @Test
    void unmatchedRoute_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/private-test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void formLoginAndHttpBasic_areDisabled() {
        boolean hasFormLoginFilter = securityFilterChain.getFilters()
                .stream()
                .anyMatch(filter ->
                        filter instanceof UsernamePasswordAuthenticationFilter);

        boolean hasBasicAuthFilter = securityFilterChain.getFilters()
                .stream()
                .anyMatch(filter ->
                        filter instanceof BasicAuthenticationFilter);

        assertFalse(hasFormLoginFilter);
        assertFalse(hasBasicAuthFilter);
    }
}
