package com.example.hexhiveint.service;

import com.example.hexhiveint.model.SensorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SensorDataServiceTest {

    private SensorDataService sensorDataService;

    @BeforeEach
    void setUp() {
        sensorDataService = new SensorDataService();
    }

    @Test
    void processSensorData_NormalState() {
        SensorData data = new SensorData();
        data.setTemperature(25.0);
        data.setAirPpm(400);
        data.setNoiseDb(45);
        data.setPirMotion(false);
        data.setDistanceCm(100.0);

        SensorData processed = sensorDataService.processSensorData(data);
        assertEquals("NORMAL", processed.getSeverity());
        assertEquals("System Nominal", processed.getMessage());
    }

    @Test
    void processSensorData_FireHazard() {
        SensorData data = new SensorData();
        data.setTemperature(50.0);
        data.setAirPpm(1500);

        SensorData processed = sensorDataService.processSensorData(data);
        assertEquals("CRITICAL", processed.getSeverity());
        assertEquals("POTENTIAL FIRE HAZARD", processed.getMessage());
    }

    @Test
    void processSensorData_HighNoise() {
        SensorData data = new SensorData();
        data.setTemperature(25.0);
        data.setAirPpm(400);
        data.setNoiseDb(90);

        SensorData processed = sensorDataService.processSensorData(data);
        assertEquals("WARNING", processed.getSeverity());
        assertEquals("High Noise Level", processed.getMessage());
    }
}
