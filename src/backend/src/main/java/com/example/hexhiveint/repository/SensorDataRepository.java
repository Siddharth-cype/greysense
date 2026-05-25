package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link SensorData} entities.
 *
 * <p>Provides CRUD operations and a derived query method for retrieving
 * the most recent sensor telemetry readings for the real-time dashboard.</p>
 */
@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    /**
     * Retrieves the 10 most recent sensor data entries ordered by timestamp descending.
     *
     * @return a list of up to 10 {@link SensorData} entities
     */
    List<SensorData> findTop10ByOrderByTimestampDesc();
}
