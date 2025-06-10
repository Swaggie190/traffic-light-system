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
public class SimulationComparison {
    private String simulationId;
    private String name;
    private TrafficScenario scenario;
    private Double vehicleWaitingTime;
    private Double pedestrianWaitingTime;
    private Double combinedIndex;
    private Integer rank;
    private Double improvementPercent; 
}
