package com.example.hexhiveint.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JPA entity representing a system event log entry.
 *
 * <p>Tracks significant system events such as sensor alerts, device
 * commands, entry/exit detections, and server lifecycle events.
 * Each log entry is auto-assigned an ID and stamped with a server timestamp.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable event description. */
    private String message;

    /** Event category: COMMAND, ENTRY, EXIT, SERVER, ALERT, etc. */
    private String type;

    /** Epoch timestamp (ms) when the event was recorded. */
    private long timestamp;
}
