package com.cp.friend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.cp.friend.model.Interest;
import com.cp.friend.service.InterestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InterestControllerMvcTest {

    @Mock
    private InterestService interestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InterestController(interestService))
                .build();
    }

    @Test
    void getInterests_withData_returnsOkAndList()
            throws Exception {

        Interest interest = new Interest();
        interest.setId(UUID.randomUUID());
        interest.setName("Music");
        interest.setActive(true);

        when(interestService.allInterests())
                .thenReturn(List.of(interest));

        mockMvc.perform(get("/api/interest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Music"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getInterests_withoutData_returnsOkAndEmptyList()
            throws Exception {

        when(interestService.allInterests())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/interest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
