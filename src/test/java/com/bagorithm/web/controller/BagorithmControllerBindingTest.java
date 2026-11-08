package com.bagorithm.web.controller;

import com.bagorithm.web.service.RegistrationService;
import com.bagorithm.web.service.RecommendationService;
import com.bagorithm.web.service.TravelToolsService;
import com.bagorithm.web.service.TripCountdownService;
import com.bagorithm.web.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BagorithmControllerBindingTest {
    private MockMvc mockMvc;
    private RegistrationService registration;

    @BeforeEach
    void setUp() {
        registration = mock(RegistrationService.class);
        BagorithmController controller = new BagorithmController(
                mock(JdbcTemplate.class),
                mock(TripService.class),
                mock(TravelToolsService.class),
                registration,
                mock(RecommendationService.class),
                mock(TripCountdownService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void bindsRegistrationRequestParametersByName() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test Traveler")
                        .param("email", "test@example.com")
                        .param("password", "TestPassword-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(registration).register("Test Traveler", "test@example.com", "TestPassword-123");
    }
}
