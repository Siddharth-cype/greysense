package com.example.hexhiveint.config;

import com.example.hexhiveint.model.AppSetting;
import com.example.hexhiveint.model.DeviceSetting;
import com.example.hexhiveint.model.EventLog;
import com.example.hexhiveint.model.UserAccount;
import com.example.hexhiveint.repository.AppSettingRepository;
import com.example.hexhiveint.repository.DeviceSettingRepository;
import com.example.hexhiveint.repository.EventLogRepository;
import com.example.hexhiveint.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Initialises the database with default seed data on first launch.
 *
 * <p>Creates SQL views for convenient dashboard queries and populates
 * the device settings, user accounts, application settings, and event
 * log tables if they are empty. This ensures the system is immediately
 * operational after a fresh deployment.</p>
 */
@Configuration
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final DeviceSettingRepository deviceRepository;
    private final EventLogRepository logRepository;
    private final UserAccountRepository userRepository;
    private final AppSettingRepository appSettingRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs the DataSeeder with required repository and JDBC dependencies.
     *
     * @param deviceRepository     repository for device settings
     * @param logRepository        repository for event logs
     * @param userRepository       repository for user accounts
     * @param appSettingRepository repository for application settings
     * @param jdbcTemplate         JDBC template for raw SQL view creation
     */
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

    /**
     * Runs on application startup. Creates SQL views and seeds default data
     * for empty tables.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if any database operation fails
     */
    @Override
    public void run(String... args) throws Exception {
        createViews();

        if (deviceRepository.count() == 0) {
            log.info("Seeding default device settings...");
            deviceRepository.save(new DeviceSetting("light", true, 80, "#fbbf24", System.currentTimeMillis()));
            deviceRepository.save(new DeviceSetting("fan", true, 3, "", System.currentTimeMillis()));
        }

        if (userRepository.count() == 0) {
            log.info("Seeding default user account...");
            userRepository.save(new UserAccount("admin", "admin"));
        }

        if (appSettingRepository.count() == 0) {
            log.info("Seeding default app settings...");
            appSettingRepository.save(new AppSetting("accentColor", "#f59e0b"));
        }

        if (logRepository.count() == 0) {
            log.info("Seeding initial system logs...");
            logRepository.save(new EventLog(null, "DATABASE INITIALIZED AND SEEDED", "SERVER", System.currentTimeMillis()));
            logRepository.save(new EventLog(null, "HEXIVE IOT GATEWAY ONLINE", "SERVER", System.currentTimeMillis() - 1000));
        }
    }

    /**
     * Creates SQL views for the dashboard if they do not already exist.
     * Views provide convenient read-only access to ordered sensor data,
     * event logs, device settings, and application settings.
     */
    private void createViews() {
        log.info("Ensuring SQL views exist...");
        jdbcTemplate.execute("CREATE VIEW IF NOT EXISTS VIEW_ALL_SENSORS AS SELECT * FROM SENSOR_DATA ORDER BY TIMESTAMP DESC");
        jdbcTemplate.execute("CREATE VIEW IF NOT EXISTS VIEW_ALL_LOGS AS SELECT * FROM EVENT_LOG ORDER BY TIMESTAMP DESC");
        jdbcTemplate.execute("CREATE VIEW IF NOT EXISTS VIEW_ALL_DEVICES AS SELECT * FROM DEVICE_SETTING");
        jdbcTemplate.execute("CREATE VIEW IF NOT EXISTS VIEW_ALL_APP_SETTINGS AS SELECT * FROM APP_SETTING");
    }
}
