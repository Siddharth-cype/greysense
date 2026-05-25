package com.example.hexhiveint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HexhiveintApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void testIngestSensorData() throws Exception {
        String jsonPayload = """
            {
                "airPpm": 120,
                "noiseDb": 45,
                "distanceCm": 150.5,
                "peoplePresent": 2,
                "pirMotion": true,
                "severity": "LOW",
                "message": "Normal operation"
            }
            """;

        mockMvc.perform(post("/api/sensors/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
