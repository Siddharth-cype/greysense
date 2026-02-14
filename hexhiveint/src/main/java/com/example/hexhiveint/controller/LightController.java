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

@RestController
@RequestMapping("/api/device")
public class LightController {

    @Autowired
    private DeviceSettingRepository repository;

    @GetMapping("/light")
    public ResponseEntity<Map<String, Integer>> getLightStatus() {
        // Fetch "light" setting from DB
        Optional<DeviceSetting> setting = repository.findById("light");
        
        int brightness = 0;
        if (setting.isPresent()) {
            DeviceSetting s = setting.get();
            // Safe check for nulls
            boolean enabled = s.isEnabled(); 
            Integer val = s.getSettingValue();
            
            if (enabled && val != null) {
                brightness = val;
            }
        }

        // Return simple JSON: { "brightness": <value> }
        return ResponseEntity.ok(Collections.singletonMap("brightness", brightness));
    }
}
