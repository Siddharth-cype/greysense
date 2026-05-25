package com.example.hexhiveint.controller;

import com.example.hexhiveint.dto.SensorDataDTO;
import com.example.hexhiveint.model.SensorData;
import com.example.hexhiveint.repository.SensorDataRepository;
import com.example.hexhiveint.service.SensorDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SensorDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SensorDataRepository sensorDataRepository;

    @MockBean
    private SensorDataService sensorDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testIngest_ValidPayload() throws Exception {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setAirPpm(400);
        dto.setNoiseDb(50);
        dto.setDistanceCm(150.0);

        SensorData savedData = new SensorData();
        savedData.setId(1L);

        Mockito.when(sensorDataRepository.save(any(SensorData.class))).thenReturn(savedData);
        Mockito.when(sensorDataService.processSensorData(any(SensorData.class))).thenReturn(savedData);

        mockMvc.perform(post("/api/sensors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void testIngest_InvalidPayload() throws Exception {
        SensorDataDTO dto = new SensorDataDTO(); // Missing required fields

        mockMvc.perform(post("/api/sensors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
