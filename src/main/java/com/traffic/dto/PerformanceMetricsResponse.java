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
public class PerformanceMetricsResponse {
    private String simulationId;
    private Long totalTimeSteps;
    private Double averageVehicleWaitingTime;
    private Double averagePedestrianWaitingTime;
    private Double combinedPerformanceIndex;
    private Long totalVehiclesProcessed;
    private Long totalPedestriansProcessed;
    private Long phase1TotalTime;
    private Long phase2TotalTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime calculatedAt;
}