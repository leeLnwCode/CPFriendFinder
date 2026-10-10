package com.cp.friend.controller;

import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.dto.response.ChatMessageResponse;
import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.model.Message;
import com.cp.friend.service.ChatMessageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatMessageControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ChatMessageController controller =
                new ChatMessageController(
                        chatMessageService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void getMessages_invalidBefore_returnsBadRequest()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                userId
        );

        mockMvc.perform(
                        get(
                                "/api/chats/{roomId}/messages",
                                roomId
                        )
                                .param(
                                        "before",
                                        "not-an-instant"
                                )
                                .session(session)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("BAD_REQUEST")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "before must be an ISO-8601 instant, e.g. 2026-01-01T00:00:00Z"
                                )
                );

        verifyNoInteractions(
                chatMessageService
        );
    }

    @Test
    void sendMessage_validationAndSession_contract()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                userId
        );

        mockMvc.perform(
                        post(
                                "/api/chats/{roomId}/messages",
                                roomId
                        )
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "content": "",
                                          "messageType": "TEXT"
                                        }
                                        """)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VALIDATION_ERROR"
                                )
                );

        ChatMessageResponse response =
                new ChatMessageResponse(
                        messageId,
                        roomId,
                        userId,
                        "Sender",
                        "User",
                        "avatar.png",
                        "hello",
                        Message.MessageType.TEXT,
                        Instant.now()
                );

        when(chatMessageService.send(
                eq(userId),
                eq(roomId),
                any(ChatMessagePayload.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post(
                                "/api/chats/{roomId}/messages",
                                roomId
                        )
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "content": "hello",
                                          "messageType": "TEXT"
                                        }
                                        """)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        messageId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.content")
                                .value("hello")
                )
                .andExpect(
                        jsonPath("$.messageType")
                                .value("TEXT")
                );

        mockMvc.perform(
                        post(
                                "/api/chats/{roomId}/messages",
                                roomId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "content": "hello",
                                          "messageType": "TEXT"
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

        verify(chatMessageService, times(1))
                .send(
                        eq(userId),
                        eq(roomId),
                        any(ChatMessagePayload.class)
                );
    }
}
