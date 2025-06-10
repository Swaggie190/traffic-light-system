package com.traffic.dto;

import com.traffic.model.TrafficPhase;
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
public class TrafficStateResponse {
    private Long timeStep;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private Integer vehiclesNorth;
    private Integer vehiclesSouth;
    private Integer vehiclesEast;
    private Integer vehiclesWest;
    private Integer pedestriansNorth;
    private Integer pedestriansSouth;
    private Integer pedestriansEast;
    private Integer pedestriansWest;
    private TrafficPhase currentPhase;
    private Integer currentGreenTime;
    private Integer calculatedGreenTime;
    private Double phase1Density;
    private Double phase2Density;
}
