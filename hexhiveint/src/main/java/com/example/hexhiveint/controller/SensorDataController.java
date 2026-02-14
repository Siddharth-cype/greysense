package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.SensorData;
import com.example.hexhiveint.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@CrossOrigin(origins = "*")
public class SensorDataController {

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private com.example.hexhiveint.service.SensorDataService sensorDataService;

    // Ingestion from ESP32
    @PostMapping({"", "/"})
    public SensorData ingest(@RequestBody SensorData payload) {
        // Server controls ID and timestamp
        payload.setId(null);
        payload.setTimestamp(System.currentTimeMillis());
        
        // Apply Decision Engine logic
        sensorDataService.processSensorData(payload);
        
        return sensorDataRepository.save(payload);
    }

    // Get all records (fixes 404 on GET /api/sensors)
    // Get all records (fixes 404 on GET /api/sensors)
    @GetMapping({"", "/"})
    public List<SensorData> getAll() {
        return sensorDataRepository.findAll();
    }

    // Read-only for dashboard
    @GetMapping("/latest")
    public List<SensorData> getLatest() {
        return sensorDataRepository.findTop10ByOrderByTimestampDesc();
    }
}
