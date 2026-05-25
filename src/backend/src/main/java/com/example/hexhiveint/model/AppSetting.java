package com.example.hexhiveint.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JPA entity representing a key-value application setting.
 *
 * <p>Stores UI preferences such as the dashboard accent colour.
 * Each setting is uniquely identified by its {@code settingKey}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AppSetting {

    /** Unique identifier for this setting (e.g., "accentColor"). */
    @Id
    private String settingKey;

    /** The value associated with this setting (e.g., "#f59e0b"). */
    private String settingValue;
}
