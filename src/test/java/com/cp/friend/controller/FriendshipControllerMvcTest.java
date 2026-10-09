package com.cp.friend.controller;

import com.cp.friend.dto.response.FriendResponse;
import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.service.FriendshipService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FriendshipControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private FriendshipService friendshipService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        FriendshipController controller =
                new FriendshipController(
                        friendshipService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void listFriends_withSession_returnsOk()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                userId
        );

        FriendResponse response =
                new FriendResponse(
                        friendId,
                        "Friend",
                        "User",
                        "avatar.png",
                        (short) 3,
                        "Computer Science",
                        "Bio",
                        List.of(),
                        Instant.now()
                );

        when(friendshipService.listFriends(userId))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/friends")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].friendId")
                                .value(
                                        friendId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].firstname")
                                .value("Friend")
                );
    }

    @Test
    void unfriend_validRequest_returnsNoContent()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                userId
        );

        mockMvc.perform(
                        delete(
                                "/api/friends/{friendId}",
                                friendId
                        )
                                .session(session)
                )
                .andExpect(
                        status().isNoContent()
                );

        verify(friendshipService)
                .unfriend(
                        userId,
                        friendId
                );
    }
}
