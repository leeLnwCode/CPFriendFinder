package com.cp.friend.controller;

import com.cp.friend.dto.response.UserDiscoveryResponse;
import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.service.UserDiscoveryService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserDiscoveryControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private UserDiscoveryService userDiscoveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UserDiscoveryController controller =
                new UserDiscoveryController(
                        userDiscoveryService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void discover_filtersAndSession_areHandled()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "userId",
                userId
        );

        UserDiscoveryResponse response =
                new UserDiscoveryResponse(
                        candidateId,
                        "Candidate",
                        "User",
                        "avatar.png",
                        3
                );

        when(userDiscoveryService.discover(
                userId,
                "CS",
                (short) 3
        )).thenReturn(
                List.of(response)
        );

        mockMvc.perform(
                        get("/api/users/discover")
                                .param(
                                        "department",
                                        "CS"
                                )
                                .param(
                                        "year",
                                        "3"
                                )
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(
                                        candidateId.toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$[0].commonInterestCount"
                        ).value(3)
                );

        verify(userDiscoveryService)
                .discover(
                        userId,
                        "CS",
                        (short) 3
                );

        mockMvc.perform(
                        get("/api/users/discover")
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
