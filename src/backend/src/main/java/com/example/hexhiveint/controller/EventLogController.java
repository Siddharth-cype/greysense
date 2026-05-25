package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.EventLog;
import com.example.hexhiveint.repository.EventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for system event log management.
 *
 * <p>Provides endpoints for retrieving the most recent system events
 * and for posting new log entries. Event logs track sensor alerts,
 * device commands, and server lifecycle events.</p>
 */
@RestController
@RequestMapping("/api/logs")
public class EventLogController {

    @Autowired
    private EventLogRepository repository;

    /**
     * Retrieves the 50 most recent event log entries, ordered by timestamp descending.
     *
     * @return a list of the latest {@link EventLog} entities
     */
    @GetMapping
    public List<EventLog> getLogs() {
        return repository.findTop50ByOrderByTimestampDesc();
    }

    /**
     * Creates a new event log entry with a server-stamped timestamp.
     *
     * @param log the event log entry from the request body
     * @return the persisted {@link EventLog} entity
     */
    @PostMapping
    public EventLog addLog(@RequestBody EventLog log) {
        log.setTimestamp(System.currentTimeMillis());
        return repository.save(log);
    }
}
