package com.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRequest {
    
    @NotBlank(message = "Simulation ID is required")
    private String simulationId;
    
    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 3600, message = "Duration must not exceed 3600 seconds")
    private Integer durationSeconds;
    
    @Min(value = 100, message = "Time step must be at least 100ms")
    @Max(value = 10000, message = "Time step must not exceed 10000ms")
    private Integer timeStepMillis;
    
    private Boolean realTimeMode;
}