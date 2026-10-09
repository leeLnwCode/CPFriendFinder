package com.cp.friend.controller;

import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        NotificationController controller =
                new NotificationController(
                        notificationService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void notificationEndpoints_enforceStatusAndSession()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                userId
        );

        when(notificationService.list(
                userId,
                false,
                0,
                20
        )).thenReturn(List.of());

        when(notificationService.unreadCount(userId))
                .thenReturn(3L);

        mockMvc.perform(
                        get("/api/notifications")
                                .session(session)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/notifications/unread-count"
                        )
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.count")
                                .value(3)
                );

        mockMvc.perform(
                        post(
                                "/api/notifications/{notificationId}/read",
                                notificationId
                        )
                                .session(session)
                )
                .andExpect(
                        status().isNoContent()
                );

        mockMvc.perform(
                        post(
                                "/api/notifications/read-all"
                        )
                                .session(session)
                )
                .andExpect(
                        status().isNoContent()
                );

        mockMvc.perform(
                        get("/api/notifications")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Authentication required"
                                )
                );

        verify(notificationService)
                .markAsRead(
                        userId,
                        notificationId
                );

        verify(notificationService)
                .markAllAsRead(userId);
    }
}
