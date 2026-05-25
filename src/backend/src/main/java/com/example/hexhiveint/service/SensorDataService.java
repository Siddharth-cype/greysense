package com.example.hexhiveint.service;

import com.example.hexhiveint.model.SensorData;
import org.springframework.stereotype.Service;

/**
 * Decision engine for sensor telemetry classification.
 *
 * <p>Analyses incoming {@link SensorData} payloads and assigns a severity
 * level ({@code NORMAL}, {@code WARNING}, {@code CRITICAL}) along with a
 * human-readable diagnostic message. The classification logic follows a
 * priority cascade:</p>
 *
 * <ol>
 *     <li><strong>Fire / Hazard Detection</strong> — extreme heat (&gt;45°C) combined with poor air quality</li>
 *     <li><strong>Air Quality Monitoring</strong> — PPM thresholds for ventilation alerts</li>
 *     <li><strong>Proximity Breach</strong> — PIR motion + ultrasonic distance &lt;50 cm</li>
 *     <li><strong>Noise Pollution</strong> — decibel readings exceeding 85 dB</li>
 * </ol>
 *
 * @see SensorData
 */
@Service
public class SensorDataService {

    /**
     * Processes a sensor data payload through the decision engine.
     *
     * <p>Evaluates environmental conditions in priority order and assigns
     * the highest applicable severity. CRITICAL conditions take precedence
     * over WARNING, and WARNING over NORMAL.</p>
     *
     * @param data the sensor telemetry payload to classify (mutated in place)
     * @return the same {@link SensorData} instance with severity and message fields populated
     */
    public SensorData processSensorData(SensorData data) {
        String severity = "NORMAL";
        String message = "System Nominal";

        // Priority 1: Fire / Hazard Detection (High Temp + Low Air Quality)
        if (data.getTemperature() != null && data.getTemperature() > 45.0) {
            severity = "CRITICAL";
            message = "EXTREME HEAT DETECTED";
            if (data.getAirPpm() != null && data.getAirPpm() > 1000) {
                message = "POTENTIAL FIRE HAZARD";
            }
        }

        // Priority 2: Air Quality Monitoring
        else if (data.getAirPpm() != null) {
            if (data.getAirPpm() >= 2000) {
                severity = "CRITICAL";
                message = "Hazardous Air Quality";
            } else if (data.getAirPpm() >= 1200 && !"CRITICAL".equals(severity)) {
                severity = "WARNING";
                message = "Poor Ventilation";
            }
        }

        // Priority 3: Proximity / Intruder Alert (Ultrasonic + PIR)
        if (Boolean.TRUE.equals(data.getPirMotion())
                && data.getDistanceCm() != null
                && data.getDistanceCm() > 0
                && data.getDistanceCm() < 50) {
            severity = "WARNING";
            message = "Proximity Breach Detected";
        }

        // Priority 4: Noise Pollution
        if (data.getNoiseDb() != null && data.getNoiseDb() > 85 && !"CRITICAL".equals(severity)) {
            severity = "WARNING";
            message = "High Noise Level";
        }

        data.setSeverity(severity);
        data.setMessage(message);

        return data;
    }
}
