package com.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonSummary {
    private String bestSimulation;
    private String worstSimulation;
    private Double averageImprovement;
    private String recommendedScenario;
    private String insights;
}