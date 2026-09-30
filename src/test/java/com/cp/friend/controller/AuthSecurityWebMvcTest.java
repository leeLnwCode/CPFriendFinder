package com.cp.friend.controller;

import com.cp.friend.config.SecurityConfig;
import com.cp.friend.dto.auth.LoginRequest;
import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the authentication routes with Spring MVC and the application's
 * SecurityFilterChain enabled. AuthService is mocked; database operations
 * and real server networking are outside the scope of this test class.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityWebMvcTest {

    private static final UUID USER_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private static final String REGISTER_JSON = """
            {"email":"student@kku.ac.th","password":"password123",
             "firstname":"Test","lastname":"Student"}
            """;

    private static final String LOGIN_JSON = """
            {"email":"student@kku.ac.th","password":"password123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setEmail("student@kku.ac.th");
        user.setFirstname("Test");
        user.setLastname("Student");
        user.setStatus(User.Status.ACTIVE);
    }

    @Test // SEC-001
    void anonymousRegistration_withoutCsrfToken_returns201() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("student@kku.ac.th"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test // SEC-002
    void anonymousLogin_withoutCsrfToken_returns200AndStoresSession() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(user);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertEquals(USER_ID, session.getAttribute("userId"));
    }

    @Test // SEC-003
    void anonymousSessionCheck_isPermittedAndReturnsNull() throws Exception {
        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"userId\":null}"));

        verifyNoInteractions(authService);
    }

    @Test // SEC-004
    void loginThenSessionCheck_returnsSameUserId() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(user);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test // SEC-005
    void loggedInUser_canLogoutWithoutCsrfToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", USER_ID);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertTrue(session.isInvalid());
        verifyNoInteractions(authService);
    }

    @Test // SEC-006
    void anonymousRegistration_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test // SEC-007
    void anonymousLogin_withoutBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test // SEC-008
    void anonymousLogout_withoutCsrfToken_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verifyNoInteractions(authService);
    }

    @Test // SEC-009
    void anonymousLogin_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
