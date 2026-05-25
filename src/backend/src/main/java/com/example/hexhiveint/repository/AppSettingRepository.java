package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AppSetting} entities.
 *
 * <p>Provides CRUD operations for application-level key-value settings.
 * Primary key is the setting key string.</p>
 */
@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
