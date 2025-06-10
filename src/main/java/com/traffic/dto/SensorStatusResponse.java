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
public class SensorStatusResponse {
    private String simulationId;
    private String inductiveLoopStatus; // ACTIVE, INACTIVE, ERROR
    private String videoDetectionStatus; // ACTIVE, INACTIVE, ERROR
    private SensorDetection pedestrianDetections;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdate;
    
    private SensorDetection vehicleDetections;
}