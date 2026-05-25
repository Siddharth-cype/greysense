package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link EventLog} entities.
 *
 * <p>Provides CRUD operations and a derived query method for retrieving
 * the most recent event log entries for dashboard display.</p>
 */
@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    /**
     * Retrieves the 50 most recent event log entries ordered by timestamp descending.
     *
     * @return a list of up to 50 {@link EventLog} entities
     */
    List<EventLog> findTop50ByOrderByTimestampDesc();
}
