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
public class SensorDataRequest {
    
    @NotBlank(message = "Simulation ID is required")
    private String simulationId;
    
    @Min(value = 0, message = "Vehicle count must be non-negative")
    private Integer vehiclesNorth;
    
    @Min(value = 0, message = "Vehicle count must be non-negative")
    private Integer vehiclesSouth;
    
    @Min(value = 0, message = "Vehicle count must be non-negative")
    private Integer vehiclesEast;
    
    @Min(value = 0, message = "Vehicle count must be non-negative")
    private Integer vehiclesWest;
    
    @Min(value = 0, message = "Pedestrian count must be non-negative")
    private Integer pedestriansNorth;
    
    @Min(value = 0, message = "Pedestrian count must be non-negative")
    private Integer pedestriansSouth;
    
    @Min(value = 0, message = "Pedestrian count must be non-negative")
    private Integer pedestriansEast;
    
    @Min(value = 0, message = "Pedestrian count must be non-negative")
    private Integer pedestriansWest;
}
