package com.cp.friend.integration;

import com.cp.friend.dto.auth.LoginRequest;
import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Full Spring web-context smoke tests for the existing page and auth routes.
 * Security filters and MVC configuration are active. AuthService is mocked;
 * these tests do not verify live PostgreSQL access or browser JavaScript.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebFlowIntegrationTest {

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

    @Test // WEB-001
    void anonymousUser_canOpenLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test // WEB-002
    void anonymousUser_canOpenRegistrationPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test // WEB-003
    void anonymousUser_cannotOpenProtectedHomePage() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is4xxClientError());
    }

    @Test // WEB-004
    void registrationStylesheet_isPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/css/register.css"))
                .andExpect(status().isOk());
    }

    @Test // WEB-005
    void registrationJavascript_isPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/js/register.js"))
                .andExpect(status().isOk());
    }

    @Test // WEB-006
    void homeImage_isPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/images/man.jpg"))
                .andExpect(status().isOk());
    }

    @Test // WEB-007
    void registrationApi_withRealWebContext_returnsPublicUserData() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("student@kku.ac.th"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test // WEB-008
    void loginSessionAndLogout_workAcrossRequests() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(user);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@kku.ac.th"));

        assertEquals(USER_ID, session.getAttribute("userId"));

        mockMvc.perform(get("/api/auth").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());

        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"userId\":null}"));
    }
}
