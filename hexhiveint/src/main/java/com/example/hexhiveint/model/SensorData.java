package com.example.hexhiveint.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "SENSOR_DATA")
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Matches ESP32 JSON payload fields
    private Integer airPpm;
    private Integer noiseDb;
    private Double distanceCm;
    private Integer peoplePresent;
    private Boolean pirMotion;
    private String severity;
    private String message;

    // New fields from Arduino
    private Double temperature;
    private Integer humidity;
    private Integer totalEntered;
    private Integer totalLeft;

    // ESP32 Analog Raw Data
    // private Integer micAnalog; // Removed as per latest Arduino code (sends noiseDb)
    // private Integer airAnalog; // Removed as per latest Arduino code (sends airPpm)
    private Integer ledLevel;

    // Missing fields for Frontend
    private Integer lightLevel;
    private Double vibration;

    // Server-side timestamp
    private Long timestamp;

    // Raw Microphone Data (User Request)
    private Integer micRaw;
    private Integer micBaseline;
}
