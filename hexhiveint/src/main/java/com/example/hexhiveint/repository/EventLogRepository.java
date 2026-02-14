package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findTop50ByOrderByTimestampDesc();
}
