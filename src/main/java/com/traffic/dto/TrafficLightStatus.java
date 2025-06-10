package com.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficLightStatus {
    private String simulationId;
    private Long timeStep;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // Light states for each direction
    private LightState northSouth;
    private LightState eastWest;
    
    // Timing information
    private Integer currentGreenTime;
    private Integer remainingGreenTime;
    private Integer nextPhaseCountdown;
    
    // Current densities
    private Double northSouthDensity;
    private Double eastWestDensity;
}