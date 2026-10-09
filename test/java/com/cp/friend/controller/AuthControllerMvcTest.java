package com.cp.friend.controller;

import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.model.User;
import com.cp.friend.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        AuthController controller =
                new AuthController(authService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private User user(UUID id) {
        User user = new User();
        setUserId(user, id);
        user.setEmail("test@kku.ac.th");
        user.setFirstname("Jiratchaya");
        user.setLastname("Paocanthuek");
        user.setImageUrl("avatar.png");
        user.setStatus(User.Status.ACTIVE);
        user.setPasswordHash("encoded-password");
        return user;
    }

    @Test
    void register_validRequest_returnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();

        when(authService.register(any()))
                .thenReturn(user(userId));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password": "12345678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("test@kku.ac.th"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(authService).register(any());
    }

    @Test
    void login_validRequest_returnsOkAndStoresSession() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        when(authService.login(any()))
                .thenReturn(user);

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password": "12345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("test@kku.ac.th"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        org.junit.jupiter.api.Assertions.assertEquals(
                userId,
                session.getAttribute("userId")
        );
    }

    @Test
    void logout_invalidatesSessionAndReturnsNoContent() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

    @Test
    void login_invalidEmail_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "12345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("email"));

        verifyNoInteractions(authService);
    }

    @Test
    void login_blankPassword_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("password"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_shortPassword_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password": "1234567"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("password"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_futureDateOfBirth_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password": "12345678",
                                  "dateOfBirth": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("dateOfBirth"));

        verifyNoInteractions(authService);
    }

    @Test
    void malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password":
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_serviceRejectsCredentials_returnsBadRequestResponse()
            throws Exception {

        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid email or password"
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password": "wrongpass"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.code")
                        .value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }

    @Test
    void register_duplicateEmail_returnsBadRequestResponse()
            throws Exception {

        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException(
                        "Email already exists"
                ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@kku.ac.th",
                                  "password": "12345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.code")
                        .value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Email already exists"));
    }

    private void setUserId(User user, UUID id) {
        try {
            java.lang.reflect.Field field =
                    User.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
