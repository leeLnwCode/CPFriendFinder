package com.cp.friend.controller;

import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.mapper.InterestMapper;
import com.cp.friend.model.Interest;
import com.cp.friend.service.UserInterestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserInterestControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private UserInterestService userInterestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UserInterestController controller =
                new UserInterestController(
                        userInterestService,
                        new InterestMapper()
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    private Interest interest(
            UUID id,
            String name
    ) {
        Interest interest = new Interest();
        interest.setId(id);
        interest.setName(name);
        interest.setActive(true);
        return interest;
    }

    @Test
    void getInterests_withSession_returnsInterestDtos()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);

        when(userInterestService.getInterests(userId))
                .thenReturn(List.of(
                        interest(interestId, "Java")
                ));

        mockMvc.perform(
                        get("/api/users/me/interests")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(interestId.toString()))
                .andExpect(jsonPath("$[0].name")
                        .value("Java"))
                .andExpect(jsonPath("$[0].isActive")
                        .value(true));
    }

    @Test
    void getInterests_withoutSession_returnsUnauthorized()
            throws Exception {

        mockMvc.perform(
                        get("/api/users/me/interests")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication required"));

        verifyNoInteractions(userInterestService);
    }

    @Test
    void replaceInterests_validIds_returnsUpdatedList()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);

        when(userInterestService.replaceInterests(
                eq(userId),
                anyList()
        )).thenReturn(List.of(
                interest(interestId, "Java")
        ));

        mockMvc.perform(
                        put("/api/users/me/interests")
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "interestIds": [
                                            "%s"
                                          ]
                                        }
                                        """.formatted(interestId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(interestId.toString()))
                .andExpect(jsonPath("$[0].name")
                        .value("Java"));
    }

    @Test
    void replaceInterests_emptyList_returnsEmptyList()
            throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);

        when(userInterestService.replaceInterests(
                userId,
                List.of()
        )).thenReturn(List.of());

        mockMvc.perform(
                        put("/api/users/me/interests")
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "interestIds": []
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void replaceInterests_nullInterestIds_returnsValidationError()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                "userId",
                UUID.randomUUID()
        );

        mockMvc.perform(
                        put("/api/users/me/interests")
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "interestIds": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(userInterestService);
    }

    @Test
    void replaceInterests_nullItem_returnsValidationError()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                "userId",
                UUID.randomUUID()
        );

        mockMvc.perform(
                        put("/api/users/me/interests")
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "interestIds": [null]
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(userInterestService);
    }

    @Test
    void replaceInterests_withoutSession_returnsUnauthorized()
            throws Exception {

        UUID interestId = UUID.randomUUID();

        mockMvc.perform(
                        put("/api/users/me/interests")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "interestIds": [
                                            "%s"
                                          ]
                                        }
                                        """.formatted(interestId))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication required"));

        verifyNoInteractions(userInterestService);
    }

    @Test
    void replaceInterests_invalidInterest_returnsBadRequest()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);

        when(userInterestService.replaceInterests(
                eq(userId),
                anyList()
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "One or more interests do not exist or are inactive"
                )
        );

        mockMvc.perform(
                        put("/api/users/me/interests")
                                .session(session)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "interestIds": [
                                            "%s"
                                          ]
                                        }
                                        """.formatted(interestId))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "One or more interests do not exist or are inactive"
                        ));
    }
}
