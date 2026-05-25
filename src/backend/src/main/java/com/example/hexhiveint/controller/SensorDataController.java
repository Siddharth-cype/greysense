package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.SensorData;
import com.example.hexhiveint.repository.SensorDataRepository;
import com.example.hexhiveint.service.SensorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.example.hexhiveint.dto.SensorDataDTO;

/**
 * REST controller for sensor telemetry ingestion and retrieval.
 *
 * <p>Provides POST endpoints for ESP32 edge nodes to submit telemetry
 * payloads, and GET endpoints for the dashboard to retrieve historical
 * and real-time sensor readings.</p>
 */
@RestController
@RequestMapping("/api/sensors")
@CrossOrigin(origins = "*")
public class SensorDataController {

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private SensorDataService sensorDataService;

    /**
     * Ingests a sensor data payload from an ESP32 edge node.
     *
     * <p>Resets the entity ID to ensure server-side generation, stamps the
     * current server time, runs the payload through the decision engine,
     * and persists it to the database.</p>
     *
     * @param payload the deserialized sensor data from the request body
     * @return the persisted {@link SensorData} entity with generated ID and severity
     */
    @PostMapping({"", "/"})
    public SensorData ingest(@Valid @RequestBody SensorDataDTO dto) {
        SensorData payload = new SensorData();
        payload.setAirPpm(dto.getAirPpm());
        payload.setNoiseDb(dto.getNoiseDb());
        payload.setDistanceCm(dto.getDistanceCm());
        payload.setPeoplePresent(dto.getPeoplePresent());
        payload.setPirMotion(dto.getPirMotion());
        payload.setTemperature(dto.getTemperature());
        payload.setHumidity(dto.getHumidity());
        payload.setTotalEntered(dto.getTotalEntered());
        payload.setTotalLeft(dto.getTotalLeft());
        payload.setLedLevel(dto.getLedLevel());
        payload.setLightLevel(dto.getLightLevel());
        payload.setVibration(dto.getVibration());
        payload.setMicRaw(dto.getMicRaw());
        payload.setMicBaseline(dto.getMicBaseline());

        payload.setId(null);
        payload.setTimestamp(System.currentTimeMillis());
        sensorDataService.processSensorData(payload);
        return sensorDataRepository.save(payload);
    }

    /**
     * Retrieves all sensor data records.
     *
     * @return a list of all {@link SensorData} entities in the database
     */
    @GetMapping({"", "/"})
    public List<SensorData> getAll() {
        return sensorDataRepository.findAll();
    }

    /**
     * Retrieves the 10 most recent sensor readings for the dashboard.
     *
     * @return a list of the latest 10 {@link SensorData} entities, ordered by timestamp descending
     */
    @GetMapping("/latest")
    public List<SensorData> getLatest() {
        return sensorDataRepository.findTop10ByOrderByTimestampDesc();
    }
}
