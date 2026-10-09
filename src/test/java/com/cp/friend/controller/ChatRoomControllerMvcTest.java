package com.cp.friend.controller;

import com.cp.friend.dto.request.CreateChatRoomRequest;
import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.service.ChatRoomService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatRoomControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ChatRoomController controller =
                new ChatRoomController(
                        chatRoomService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void roomEndpoints_statusAndSession_contract()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                userId
        );

        mockMvc.perform(
                        post("/api/chats")
                                .session(session)
                                .contentType("application/json")
                                .content("""
                                        {
                                          "roomName": "Study Room"
                                        }
                                        """)
                )
                .andExpect(
                        status().isCreated()
                );

        verify(chatRoomService)
                .createGroupRoom(
                        eq(userId),
                        any(CreateChatRoomRequest.class)
                );

        mockMvc.perform(
                        post(
                                "/api/chats/{roomId}/join",
                                roomId
                        )
                                .session(session)
                )
                .andExpect(status().isOk());

        verify(chatRoomService)
                .joinRoom(
                        userId,
                        roomId,
                        null
                );

        mockMvc.perform(
                        post(
                                "/api/chats/{roomId}/leave",
                                roomId
                        )
                                .session(session)
                )
                .andExpect(
                        status().isNoContent()
                );

        verify(chatRoomService)
                .leaveRoom(
                        userId,
                        roomId
                );

        mockMvc.perform(
                        post(
                                "/api/chats/direct/{friendId}",
                                friendId
                        )
                                .session(session)
                )
                .andExpect(status().isOk());

        verify(chatRoomService)
                .getOrCreateDirectRoom(
                        userId,
                        friendId
                );

        mockMvc.perform(
                        post("/api/chats")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "roomName": "Study Room"
                                        }
                                        """)
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
    }
}
