package com.cp.friend.controller;

import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.mapper.UserMapper;
import com.cp.friend.model.User;
import com.cp.friend.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UserController controller =
                new UserController(userService, new UserMapper());

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private User user() {
        User user = new User();
        user.setEmail("test@kku.ac.th");
        user.setFirstname("Jiratchaya");
        user.setLastname("Paocanthuek");
        user.setStatus(User.Status.ACTIVE);
        return user;
    }

    @Test
    void getMe_withSession_returnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setMaxInactiveInterval(75);

        when(userService.getProfile(userId))
                .thenReturn(user());

        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Session-Timeout-Seconds", "75"))
                .andExpect(jsonPath("$.email")
                        .value("test@kku.ac.th"))
                .andExpect(jsonPath("$.firstname")
                        .value("Jiratchaya"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getMe_withoutSession_returnsUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication required"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateMe_validRequest_returnsUpdatedProfile()
            throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);

        User updated = user();
        updated.setFirstname("New");

        when(userService.updateProfile(
                eq(userId),
                any()
        )).thenReturn(updated);

        mockMvc.perform(post("/api/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstname": "New"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname")
                        .value("New"));
    }

    @Test
    void updateMe_futureDate_returnsValidationError()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        mockMvc.perform(post("/api/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateOfBirth": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateMe_invalidImageBase64_returnsValidationError()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        mockMvc.perform(post("/api/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageBase64": "not-base64!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(userService);
    }
}
