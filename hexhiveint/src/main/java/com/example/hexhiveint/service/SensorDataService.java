package com.example.hexhiveint.service;

// This service previously generated simulated sensor data.
// It is no longer used now that real telemetry is ingested from ESP32
// through the SensorDataController. The class is kept empty to avoid
// breaking references, but can be safely deleted if unused elsewhere.
    
import com.example.hexhiveint.model.SensorData;
import org.springframework.stereotype.Service;

@Service
public class SensorDataService {

    public SensorData processSensorData(SensorData data) {
        // Default status
        String severity = "NORMAL";
        String message = "System Nominal";

        // 1. Fire / Hazard Detection (High Temp + Low Air Quality)
        if (data.getTemperature() != null && data.getTemperature() > 45.0) {
            severity = "CRITICAL";
            message = "EXTREME HEAT DETECTED";
            if (data.getAirPpm() != null && data.getAirPpm() > 1000) {
                message = "POTENTIAL FIRE HAZARD";
            }
        }

        // 2. Air Quality Monitoring
        // Override if Fire not detected, but Air is bad
        else if (data.getAirPpm() != null) {
            if (data.getAirPpm() >= 2000) {
                severity = "CRITICAL";
                message = "Hazardous Air Quality";
            } else if (data.getAirPpm() >= 1200) {
                if (!severity.equals("CRITICAL")) {
                    severity = "WARNING";
                    message = "Poor Ventilation";
                }
            }
        }

        // 3. Proximity / Intruder Alert (Ultrasonic + PIR)
        // If PIR detects motion AND Ultrasonic detects something very close (< 50cm)
        if (Boolean.TRUE.equals(data.getPirMotion()) && 
            data.getDistanceCm() != null && data.getDistanceCm() > 0 && data.getDistanceCm() < 50) {
            severity = "WARNING"; // elevate to warning
            message = "Proximity Breach Detected";
        }

        // 4. Noise Pollution
        if (data.getNoiseDb() != null && data.getNoiseDb() > 85) {
             if (!severity.equals("CRITICAL")) {
                 severity = "WARNING";
                 message = "High Noise Level";
             }
        }

        // 5. Raw Mic Data Check (Optional Sanitization)
        // If raw mic is hitting rails (0 or 4095), maybe flag it?
        // User wanted to see "actual" data, so we don't suppress it here, just categorize it.
        
        data.setSeverity(severity);
        data.setMessage(message);
        
        return data;
    }
}
