package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.DeviceSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link DeviceSetting} entities.
 *
 * <p>Provides CRUD operations for IoT device configurations.
 * Primary key is the device identifier string (e.g., "light", "fan").</p>
 */
@Repository
public interface DeviceSettingRepository extends JpaRepository<DeviceSetting, String> {
}
