package com.cp.friend.controller;

import com.cp.friend.dto.response.FriendRequestResponse;
import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.service.FriendRequestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FriendRequestControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private FriendRequestService friendRequestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        FriendRequestController controller =
                new FriendRequestController(
                        friendRequestService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void send_validRequest_returnsCreated()
            throws Exception {

        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                senderId
        );

        FriendRequestResponse response =
                new FriendRequestResponse(
                        requestId,
                        receiverId,
                        "Receiver",
                        "User",
                        "avatar.png",
                        (short) 3,
                        "Computer Science",
                        "Bio",
                        List.of(),
                        FriendRequest.Status.PENDING,
                        Instant.now()
                );

        when(friendRequestService.send(
                senderId,
                receiverId
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/friend-requests")
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "receiverId": "%s"
                                        }
                                        """.formatted(receiverId))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        requestId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(
                                        receiverId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.firstname")
                                .value("Receiver")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );
    }

    @Test
    void send_invalidInputOrSession_isRejected()
            throws Exception {

        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                senderId
        );

        mockMvc.perform(
                        post("/api/friend-requests")
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "receiverId": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VALIDATION_ERROR"
                                )
                );

        mockMvc.perform(
                        post("/api/friend-requests")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "receiverId": "%s"
                                        }
                                        """.formatted(receiverId))
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

        verifyNoInteractions(
                friendRequestService
        );
    }
}
