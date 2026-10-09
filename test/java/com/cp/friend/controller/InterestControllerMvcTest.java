package com.cp.friend.controller;

import com.cp.friend.exception.GlobalExceptionHandler;
import com.cp.friend.mapper.InterestMapper;
import com.cp.friend.model.Interest;
import com.cp.friend.service.InterestService;

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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InterestControllerMvcTest {

    private MockMvc mockMvc;

    @Mock
    private InterestService interestService;

    private InterestMapper interestMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        interestMapper = new InterestMapper();

        InterestController controller =
                new InterestController(
                        interestService,
                        interestMapper
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Interest interest(String name) {
        Interest interest = new Interest();
        interest.setId(UUID.randomUUID());
        interest.setName(name);
        interest.setActive(true);
        return interest;
    }

    @Test
    void listInterests_withoutSearch_returnsAll() throws Exception {
        Interest java = interest("Java");
        Interest web = interest("Web");

        when(interestService.allInterests())
                .thenReturn(List.of(java, web));

        mockMvc.perform(get("/api/interests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Java"))
                .andExpect(jsonPath("$[1].name")
                        .value("Web"));

        verify(interestService).allInterests();
        verify(interestService, never())
                .searchInterests(anyString());
    }

    @Test
    void listInterests_blankSearch_returnsAll() throws Exception {
        when(interestService.allInterests())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/interests")
                        .param("search", "   "))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(interestService).allInterests();
        verify(interestService, never())
                .searchInterests(anyString());
    }

    @Test
    void listInterests_withSearch_returnsMatches() throws Exception {
        Interest java = interest("Java");

        when(interestService.searchInterests("java"))
                .thenReturn(List.of(java));

        mockMvc.perform(get("/api/interests")
                        .param("search", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Java"))
                .andExpect(jsonPath("$[0].isActive")
                        .value(true));

        verify(interestService)
                .searchInterests("java");
    }

    @Test
    void createInterest_validRequest_returnsCreated()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        Interest java = interest("Java");

        when(interestService.createInterest("Java"))
                .thenReturn(java);

        mockMvc.perform(post("/api/interests")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(java.getId().toString()))
                .andExpect(jsonPath("$.name")
                        .value("Java"))
                .andExpect(jsonPath("$.isActive")
                        .value(true));

        verify(interestService)
                .createInterest("Java");
    }

    @Test
    void createInterest_withoutSession_returnsUnauthorized()
            throws Exception {

        mockMvc.perform(post("/api/interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.message")
                        .value("Authentication required"));

        verifyNoInteractions(interestService);
    }

    @Test
    void createInterest_blankName_returnsValidationError()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        mockMvc.perform(post("/api/interests")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("name"));

        verifyNoInteractions(interestService);
    }

    @Test
    void createInterest_nameOver100Characters_returnsValidationError()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        String longName = "A".repeat(101);

        mockMvc.perform(post("/api/interests")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("name"));

        verifyNoInteractions(interestService);
    }

    @Test
    void createInterest_duplicate_returnsConflict()
            throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", UUID.randomUUID());

        when(interestService.createInterest("Java"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Interest already exists"
                ));

        mockMvc.perform(post("/api/interests")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.message")
                        .value("Interest already exists"));
    }
}
