// Update your SimulationRequest.java to make simulationId optional for validation
// since it's set by the controller from the path variable:

package com.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRequest {
    
    // Remove @NotBlank since this is set by the controller from path variable
    private String simulationId;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 3600, message = "Duration must not exceed 3600 seconds")
    private Integer durationSeconds;
    
    @NotNull(message = "Time step is required")
    @Min(value = 100, message = "Time step must be at least 100ms")
    @Max(value = 10000, message = "Time step must not exceed 10000ms")
    private Integer timeStepMillis;
    
    @Builder.Default
    private Boolean realTimeMode = true;
}