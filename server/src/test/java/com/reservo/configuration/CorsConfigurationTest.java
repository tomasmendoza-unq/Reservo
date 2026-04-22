package com.reservo.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.reservo.service.ResetService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResetService resetService;

    @AfterEach
    void tearDown() {
        resetService.resetAll();
    }

    @Test
    void shouldAllowCorsFromLocalhost() throws Exception {
        mockMvc.perform(get("/property")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldAllowCredentialsInCorsResponse() throws Exception {
        mockMvc.perform(get("/property")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void shouldAllowAllHttpMethodsInCors() throws Exception {
        mockMvc.perform(options("/property")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void shouldAllowCrossOriginOnMultipleEndpoints() throws Exception {
        mockMvc.perform(get("/auth")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));

        mockMvc.perform(get("/property")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));

        mockMvc.perform(get("/mis-reservas/aceptadas/1")
                .header("Origin", "http://localhost:5173"))
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldAllowAllHeadersInCors() throws Exception {
        mockMvc.perform(options("/property")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    void shouldExposeCoreHeadersInCors() throws Exception {
        mockMvc.perform(get("/property")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Expose-Headers"));
    }
}
