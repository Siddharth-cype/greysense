package com.example.hexhiveint.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a single sensor telemetry reading.
 *
 * <p>Maps to the {@code SENSOR_DATA} table and stores real-time measurements
 * from ESP32 edge nodes including air quality, noise levels, temperature,
 * humidity, ultrasonic distance, PIR motion, and occupancy counts.</p>
 *
 * <p>The {@code severity} and {@code message} fields are populated server-side
 * by the {@link com.example.hexhiveint.service.SensorDataService} decision engine.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "SENSOR_DATA")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Air quality reading in parts per million (PPM). */
    private Integer airPpm;

    /** Calibrated noise level in decibels (dB). */
    private Integer noiseDb;

    /** Ultrasonic distance reading in centimetres. Negative values indicate out-of-range. */
    private Double distanceCm;

    /** Current occupancy count (totalEntered - totalLeft). */
    private Integer peoplePresent;

    /** PIR motion sensor state: {@code true} if motion is detected. */
    private Boolean pirMotion;

    /** Decision engine severity classification: NORMAL, WARNING, or CRITICAL. */
    private String severity;

    /** Decision engine diagnostic message describing the current system state. */
    private String message;

    /** Ambient temperature in degrees Celsius. */
    private Double temperature;

    /** Relative humidity percentage. */
    private Integer humidity;

    /** Cumulative entry count from PIR sensor. */
    private Integer totalEntered;

    /** Cumulative exit count from ultrasonic sensor. */
    private Integer totalLeft;

    /** Current LED brightness level (0–255) as reported by the ESP32. */
    private Integer ledLevel;

    /** Ambient light level for environmental monitoring. */
    private Integer lightLevel;

    /** Vibration sensor reading for structural monitoring. */
    private Double vibration;

    /** Server-side epoch timestamp in milliseconds. */
    private Long timestamp;

    /** Raw microphone ADC amplitude value from the ESP32. */
    private Integer micRaw;

    /** Adaptive noise floor baseline computed by the ESP32. */
    private Integer micBaseline;
}
