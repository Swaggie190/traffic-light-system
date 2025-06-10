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
public class SimulationStatusResponse {
    private String simulationId;
    private String status; // IDLE, RUNNING, PAUSED, COMPLETED, ERROR
    private Long currentTimeStep;
    private Long totalTimeSteps;
    private Double progress;
    private TrafficStateResponse currentState;
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdate;
}