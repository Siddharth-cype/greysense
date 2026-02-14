package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.DeviceSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceSettingRepository extends JpaRepository<DeviceSetting, String> {
}
