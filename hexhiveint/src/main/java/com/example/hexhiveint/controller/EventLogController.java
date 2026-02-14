package com.example.hexhiveint.controller;

import com.example.hexhiveint.model.EventLog;
import com.example.hexhiveint.repository.EventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class EventLogController {

    @Autowired
    private EventLogRepository repository;

    @GetMapping
    public List<EventLog> getLogs() {
        return repository.findTop50ByOrderByTimestampDesc();
    }

    @PostMapping
    public EventLog addLog(@RequestBody EventLog log) {
        log.setTimestamp(System.currentTimeMillis());
        return repository.save(log);
    }
}
