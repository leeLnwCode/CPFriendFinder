package com.cp.friend.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cp.friend.controller.AuthController;
import com.cp.friend.mapper.UserMapper;
import com.cp.friend.service.AuthService;
import com.cp.friend.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void publicPages_areNotBlockedBySecurity() throws Exception {
        String[] paths = {
                "/",
                "/login",
                "/home",
                "/register",
                "/room",
                "/setting",
                "/notification",
                "/friend",
                "/random"
        };

        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(
                                status != 401 && status != 403,
                                path + " should not be blocked by security"
                        );
                    });
        }
    }

    @Test
    void apiLogin_withoutAuthentication_reachesControllerLayer()
            throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiPost_doesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void websocketRoutes_areNotBlockedBySecurity() throws Exception {
        String[] paths = {
                "/ws",
                "/ws/test"
        };

        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(
                                status != 401 && status != 403,
                                path + " should not be blocked by security"
                        );
                    });
        }
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
    void csrfFilter_isDisabled() {
        boolean hasCsrfFilter = securityFilterChain.getFilters()
                .stream()
                .anyMatch(filter -> filter instanceof CsrfFilter);

        assertFalse(hasCsrfFilter);
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

    @Test
    void passwordEncoder_usesSecureHashing() {
        String rawPassword = "12345678";
        String encoded = passwordEncoder.encode(rawPassword);

        assertFalse(rawPassword.equals(encoded));
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }
}
