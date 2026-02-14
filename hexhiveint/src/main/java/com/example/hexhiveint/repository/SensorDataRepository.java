package com.example.hexhiveint.repository;

import com.example.hexhiveint.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    // H2-compatible derived query method
    List<SensorData> findTop10ByOrderByTimestampDesc();
}
