package com.example.hexhiveint.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class DeviceSetting {
    @Id
    private String deviceId; // e.g., "light", "fan"

    private boolean enabled;
    private int settingValue; // Brightness (0-100) or Fan Speed (0-5)
    private String color; // Hex color for light
    private long lastUpdated;
}
