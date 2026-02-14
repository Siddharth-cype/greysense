package com.example.hexhiveint.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import com.example.hexhiveint.model.AppSetting;
import com.example.hexhiveint.model.DeviceSetting;
import com.example.hexhiveint.model.EventLog;
import com.example.hexhiveint.model.UserAccount;
import com.example.hexhiveint.repository.AppSettingRepository;
import com.example.hexhiveint.repository.EventLogRepository;
import com.example.hexhiveint.repository.UserAccountRepository;
import com.example.hexhiveint.repository.DeviceSettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Random;

@Configuration
public class DataSeeder implements CommandLineRunner {

    private final DeviceSettingRepository deviceRepository;
    private final EventLogRepository logRepository;
    private final UserAccountRepository userRepository;
    private final AppSettingRepository appSettingRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    public DataSeeder(DeviceSettingRepository deviceRepository,
            EventLogRepository logRepository,
            UserAccountRepository userRepository,
            AppSettingRepository appSettingRepository,
            JdbcTemplate jdbcTemplate) {
        this.deviceRepository = deviceRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.appSettingRepository = appSettingRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        createViews();
        if (deviceRepository.count() == 0) {
            System.out.println("Seeding default device settings...");
            deviceRepository.save(new DeviceSetting("light", true, 80, "#fbbf24", System.currentTimeMillis()));
            deviceRepository.save(new DeviceSetting("fan", true, 3, "", System.currentTimeMillis()));
        }

        if (userRepository.count() == 0) {
            System.out.println("Seeding default user account...");
            userRepository.save(new UserAccount("admin", "admin"));
        }

        if (appSettingRepository.count() == 0) {
            System.out.println("Seeding default app settings...");
            appSettingRepository.save(new AppSetting("accentColor", "#f59e0b")); // Default Amber
        }

        if (logRepository.count() == 0) {
            System.out.println("Seeding initial system logs...");
            logRepository
                    .save(new EventLog(null, "DATABASE INITIALIZED AND SEEDED", "SERVER", System.currentTimeMillis()));
            logRepository.save(
                    new EventLog(null, "GREY SENSE IOT GATEWAY ONLINE", "SERVER", System.currentTimeMillis() - 1000));
        }
    }

    private void createViews() {
        System.out.println("Ensuring SQL Views exist...");
        jdbcTemplate.execute(
                "CREATE VIEW IF NOT EXISTS VIEW_ALL_SENSORS AS SELECT * FROM SENSOR_DATA ORDER BY TIMESTAMP DESC");
        jdbcTemplate
                .execute("CREATE VIEW IF NOT EXISTS VIEW_ALL_LOGS AS SELECT * FROM EVENT_LOG ORDER BY TIMESTAMP DESC");
        jdbcTemplate.execute("CREATE VIEW IF NOT EXISTS VIEW_ALL_DEVICES AS SELECT * FROM DEVICE_SETTING");
        jdbcTemplate.execute("CREATE VIEW IF NOT EXISTS VIEW_ALL_APP_SETTINGS AS SELECT * FROM APP_SETTING");
    }

    private void seedData() {
        // No-op: sensor data is now ingested live from ESP32.
    }
}
