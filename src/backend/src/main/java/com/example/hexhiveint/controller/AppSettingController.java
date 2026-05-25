package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.AppSetting;
import com.example.hexhiveint.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing application-level settings.
 *
 * <p>Provides endpoints for retrieving and updating UI preferences
 * such as the dashboard accent color. Settings are persisted as
 * key-value pairs in the {@code APP_SETTING} table.</p>
 */
@RestController
@RequestMapping("/api/app-settings")
public class AppSettingController {

    @Autowired
    private AppSettingRepository repository;

    /**
     * Retrieves all application settings.
     *
     * @return a list of all {@link AppSetting} key-value pairs
     */
    @GetMapping
    public List<AppSetting> getSettings() {
        return repository.findAll();
    }

    /**
     * Creates or updates an application setting.
     *
     * @param setting the setting key-value pair from the request body
     * @return the persisted {@link AppSetting} entity
     */
    @PostMapping
    public AppSetting updateSetting(@RequestBody AppSetting setting) {
        return repository.save(setting);
    }
}
