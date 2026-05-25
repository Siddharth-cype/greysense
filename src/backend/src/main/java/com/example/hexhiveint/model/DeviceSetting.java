package com.example.hexhiveint.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing the configuration of a controllable IoT device.
 *
 * <p>Each device (e.g., "light", "fan") has an enabled state, a numeric
 * setting value (brightness or speed), an optional hex colour, and a
 * timestamp of the last update.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class DeviceSetting {

    /** Unique device identifier (e.g., "light", "fan"). */
    @Id
    private String deviceId;

    /** Whether the device is currently active. */
    private boolean enabled;

    /** Numeric setting: brightness (0–100) for lights, speed (0–5) for fans. */
    private int settingValue;

    /** Hex colour code for light devices (e.g., "#fbbf24"). Empty for non-light devices. */
    private String color;

    /** Epoch timestamp (ms) of the last configuration change. */
    private long lastUpdated;
}
