package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.DeviceSetting;
import com.example.hexhiveint.repository.DeviceSettingRepository;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing IoT device settings (light, fan, etc.).
 *
 * <p>Handles CRUD operations for device configurations and publishes
 * state changes to AWS IoT Core over MQTT so that connected ESP32
 * edge nodes receive real-time control commands.</p>
 */
@RestController
@RequestMapping("/api/settings")
public class DeviceSettingController {

    private static final Logger log = LoggerFactory.getLogger(DeviceSettingController.class);

    @Autowired
    private DeviceSettingRepository repository;

    @Autowired(required = false)
    private MqttClient mqttClient;

    /**
     * Retrieves all device settings.
     *
     * @return a list of all {@link DeviceSetting} entities
     */
    @GetMapping
    public List<DeviceSetting> getAllSettings() {
        return repository.findAll();
    }

    /**
     * Updates a device setting and publishes the change to AWS IoT Core.
     *
     * <p>If the updated device is the light and the MQTT client is connected,
     * a JSON control message is published to the {@code greysense/control} topic
     * with the new brightness value (0 if disabled).</p>
     *
     * @param setting the updated device setting from the request body
     * @return the persisted {@link DeviceSetting} entity
     */
    @PostMapping
    public DeviceSetting updateSetting(@RequestBody DeviceSetting setting) {
        setting.setLastUpdated(System.currentTimeMillis());
        DeviceSetting saved = repository.save(setting);

        if ("light".equalsIgnoreCase(setting.getDeviceId()) && mqttClient != null && mqttClient.isConnected()) {
            try {
                int brightness = setting.isEnabled() ? setting.getSettingValue() : 0;
                String payload = String.format("{\"led\": %d}", brightness);

                MqttMessage msg = new MqttMessage(payload.getBytes());
                msg.setQos(1);
                mqttClient.publish("greysense/control", msg);
                log.info("Published LED control command: {}", payload);
            } catch (Exception e) {
                log.error("Failed to publish MQTT control message: {}", e.getMessage(), e);
            }
        }

        return saved;
    }
}
