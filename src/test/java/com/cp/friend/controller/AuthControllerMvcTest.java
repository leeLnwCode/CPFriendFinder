package com.cp.friend.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.cp.friend.dto.auth.LoginRequest;
import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerMvcTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .build();
    }

    @Test
    void register_validRequest_returnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@kku.ac.th",
                                  "password": "password123",
                                  "firstname": "Test",
                                  "lastname": "User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("student@kku.ac.th"))
                .andExpect(jsonPath("$.firstname").value("Test"))
                .andExpect(jsonPath("$.lastname").value("User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void login_validRequest_returnsOkAndCreatesSession()
            throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        MockHttpSession session = new MockHttpSession();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@kku.ac.th",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("student@kku.ac.th"));

        org.junit.jupiter.api.Assertions.assertEquals(
                userId,
                session.getAttribute("userId"));
    }

    @Test
    void sessionAuth_withoutLogin_returnsNullUserId()
            throws Exception {

        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(
                        org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void sessionAuth_withSession_returnsUserId()
            throws Exception {

        UUID userId = UUID.randomUUID();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);

        mockMvc.perform(get("/api/auth")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()));
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

    @Test
    void register_malformedJson_returnsBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void login_malformedJson_returnsBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verify(authService, never())
                .login(any(LoginRequest.class));
    }

    @Test
    void login_responseDoesNotExposePassword()
            throws Exception {

        User user = mockUser(UUID.randomUUID());

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@kku.ac.th",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist());
    }

    private User mockUser(UUID userId) {
        User user = org.mockito.Mockito.mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("student@kku.ac.th");
        when(user.getFirstname()).thenReturn("Test");
        when(user.getLastname()).thenReturn("User");
        when(user.getStatus()).thenReturn(User.Status.ACTIVE);
        when(user.getImageUrl()).thenReturn(
                "https://storage.test/storage/avatar.png");

        return user;
    }
}
