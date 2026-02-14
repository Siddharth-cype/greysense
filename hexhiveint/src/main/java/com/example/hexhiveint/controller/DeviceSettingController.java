package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.DeviceSetting;
import com.example.hexhiveint.repository.DeviceSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class DeviceSettingController {

    @Autowired
    private DeviceSettingRepository repository;

    @Autowired(required = false)
    private org.eclipse.paho.client.mqttv3.MqttClient mqttClient;

    @GetMapping
    public List<DeviceSetting> getAllSettings() {
        return repository.findAll();
    }

    @PostMapping
    public DeviceSetting updateSetting(@RequestBody DeviceSetting setting) {
        setting.setLastUpdated(System.currentTimeMillis());
        DeviceSetting saved = repository.save(setting);

        // Publish to AWS IoT Core if it's the light
        if ("light".equalsIgnoreCase(setting.getDeviceId()) && mqttClient != null && mqttClient.isConnected()) {
            try {
                int brightness = setting.isEnabled() ? setting.getSettingValue() : 0;
                String payload = String.format("{\"led\": %d}", brightness);
                
                org.eclipse.paho.client.mqttv3.MqttMessage msg = new org.eclipse.paho.client.mqttv3.MqttMessage(payload.getBytes());
                msg.setQos(1);
                mqttClient.publish("greysense/control", msg);
                System.out.println("PUBLISHED TO AWS: " + payload);
            } catch (Exception e) {
                System.err.println("Failed to publish to MQTT: " + e.getMessage());
            }
        }

        return saved;
    }
}
