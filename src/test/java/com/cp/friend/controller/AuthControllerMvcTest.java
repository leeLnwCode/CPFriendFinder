package com.cp.friend.controller;

import com.cp.friend.dto.auth.LoginRequest;
import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MVC tests for AuthController with a mocked AuthService.
 * These tests exercise request mapping, JSON responses and HttpSession.
 * They do not start Spring Boot, apply SecurityFilterChain or use a database.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerMvcTest {

    private static final UUID USER_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private static final String REGISTER_JSON = """
            {
              "email": "student@kku.ac.th",
              "password": "password123",
              "firstname": "Test",
              "lastname": "Student"
            }
            """;

    private static final String LOGIN_JSON = """
            {"email":"student@kku.ac.th","password":"password123"}
            """;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .build();

        user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setEmail("student@kku.ac.th");
        user.setFirstname("Test");
        user.setLastname("Student");
        user.setStatus(User.Status.ACTIVE);
    }

    @Test // CTRL-001
    void register_validBody_returnsCreatedAndPublicUserFields() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("student@kku.ac.th"))
                .andExpect(jsonPath("$.firstname").value("Test"))
                .andExpect(jsonPath("$.lastname").value("Student"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        ArgumentCaptor<RegisterRequest> captor =
                ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertEquals("student@kku.ac.th", captor.getValue().getEmail());
        assertEquals("password123", captor.getValue().getPassword());
    }

    @Test // CTRL-002
    void register_missingBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test // CTRL-003
    void register_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test // CTRL-004
    void login_validBody_returnsOkAndStoresUserIdInSession() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(user);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("student@kku.ac.th"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertEquals(USER_ID, session.getAttribute("userId"));
        ArgumentCaptor<LoginRequest> captor =
                ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());
        assertEquals("student@kku.ac.th", captor.getValue().getEmail());
    }

    @Test // CTRL-005
    void login_missingBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test // CTRL-006
    void login_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test // CTRL-007
    void sessionAuth_afterLogin_returnsStoredUserId() throws Exception {
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

        verify(authService).login(any(LoginRequest.class));
    }

    @Test // CTRL-008
    void sessionAuth_withoutLogin_returnsNullUserId() throws Exception {
        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"userId\":null}"));

        verifyNoInteractions(authService);
    }

    @Test // CTRL-009
    void logout_existingSession_returnsNoContentAndInvalidatesSession()
            throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", USER_ID);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertTrue(session.isInvalid());
        verifyNoInteractions(authService);
    }

    @Test // CTRL-010
    void logout_withoutPreviousLogin_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verifyNoInteractions(authService);
    }
}
