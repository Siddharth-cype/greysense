package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.DeviceSetting;
import com.example.hexhiveint.repository.DeviceSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for querying the current light device status.
 *
 * <p>Provides a read-only endpoint for the ESP32 firmware to poll
 * the desired LED brightness level from the server.</p>
 */
@RestController
@RequestMapping("/api/device")
public class LightController {

    @Autowired
    private DeviceSettingRepository repository;

    /**
     * Returns the current brightness level for the light device.
     *
     * <p>If the light is disabled or not found, brightness defaults to 0.</p>
     *
     * @return a JSON map containing the key {@code "brightness"} with an integer value (0–100)
     */
    @GetMapping("/light")
    public ResponseEntity<Map<String, Integer>> getLightStatus() {
        Optional<DeviceSetting> setting = repository.findById("light");

        int brightness = 0;
        if (setting.isPresent()) {
            DeviceSetting device = setting.get();
            if (device.isEnabled() && device.getSettingValue() > 0) {
                brightness = device.getSettingValue();
            }
        }

        return ResponseEntity.ok(Collections.singletonMap("brightness", brightness));
    }
}
