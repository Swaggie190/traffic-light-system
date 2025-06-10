package com.traffic.dto;

import com.traffic.model.TrafficScenario;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioTemplate {
    private String name;
    private String description;
    private TrafficScenario scenario;
    
    // Default values for the scenario
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
    
    // System defaults
    @Builder.Default
    private Integer minGreenTime = 15;
    @Builder.Default
    private Integer maxGreenTime = 60;
    @Builder.Default
    private Integer yellowTime = 3;
    @Builder.Default
    private Integer redClearanceTime = 2;
    @Builder.Default
    private Double pedestrianWeight = 0.3;
    @Builder.Default
    private Double switchingThreshold = 1.5;
    @Builder.Default
    private Double vehiclePerformanceWeight = 0.7;
    @Builder.Default
    private Double pedestrianPerformanceWeight = 0.3;
}