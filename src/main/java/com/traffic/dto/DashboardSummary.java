package com.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {
    private Integer totalSimulations;
    private Integer activeSimulations;
    private Integer completedSimulations;
    private Double averagePerformanceIndex;
    private String bestPerformingScenario;
    private SimulationConfigResponse mostRecentSimulation;
    private List<QuickStats> recentPerformance;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdate;
}