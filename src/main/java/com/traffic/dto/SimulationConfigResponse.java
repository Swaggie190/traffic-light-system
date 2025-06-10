package com.traffic.dto;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.traffic.model.TrafficScenario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationConfigResponse {
    private String simulationId;
    private String name;
    private TrafficScenario scenario;
    private Double lambdaNorth;
    private Double lambdaSouth;
    private Double lambdaEast;
    private Double lambdaWest;
    private Double muNorth;
    private Double muSouth;
    private Double muEast;
    private Double muWest;
    private Double sigmaNorth;
    private Double sigmaSouth;
    private Double sigmaEast;
    private Double sigmaWest;
    private Integer minGreenTime;
    private Integer maxGreenTime;
    private Integer yellowTime;
    private Integer redClearanceTime;
    private Double pedestrianWeight;
    private Double switchingThreshold;
    private Double vehiclePerformanceWeight;
    private Double pedestrianPerformanceWeight;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private Boolean isActive;
}