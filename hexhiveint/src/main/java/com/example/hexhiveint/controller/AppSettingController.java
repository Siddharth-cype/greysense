package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.AppSetting;
import com.example.hexhiveint.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/app-settings")
public class AppSettingController {

    @Autowired
    private AppSettingRepository repository;

    @GetMapping
    public List<AppSetting> getSettings() {
        return repository.findAll();
    }

    @PostMapping
    public AppSetting updateSetting(@RequestBody AppSetting setting) {
        return repository.save(setting);
    }
}
