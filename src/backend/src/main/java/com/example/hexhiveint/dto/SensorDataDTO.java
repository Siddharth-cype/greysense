package com.example.hexhiveint.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SensorDataDTO {

    @NotNull(message = "Air PPM cannot be null")
    @Min(value = 0, message = "Air PPM cannot be negative")
    private Integer airPpm;

    @NotNull(message = "Noise level cannot be null")
    @Min(value = 0, message = "Noise level cannot be negative")
    private Integer noiseDb;

    @NotNull(message = "Distance cannot be null")
    private Double distanceCm;

    private Integer peoplePresent;
    
    private Boolean pirMotion;
    
    private Double temperature;
    
    private Integer humidity;
    
    private Integer totalEntered;
    
    private Integer totalLeft;
    
    @Min(value = 0, message = "LED level must be between 0 and 255")
    @Max(value = 255, message = "LED level must be between 0 and 255")
    private Integer ledLevel;
    
    private Integer lightLevel;
    
    private Double vibration;
    
    private Integer micRaw;
    
    private Integer micBaseline;
}
