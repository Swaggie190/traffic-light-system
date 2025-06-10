package com.traffic.dto;

import com.traffic.model.TrafficScenario;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationConfigRequest {
    
    @NotBlank(message = "Simulation name is required")
    @Size(max = 100, message = "Simulation name must not exceed 100 characters")
    private String name;
    
    @NotNull(message = "Traffic scenario is required")
    private TrafficScenario scenario;
    
    // Vehicle arrival rates
    @DecimalMin(value = "0.0", message = "Lambda North must be non-negative")
    @DecimalMax(value = "10.0", message = "Lambda North must not exceed 10.0")
    private Double lambdaNorth;
    
    @DecimalMin(value = "0.0", message = "Lambda South must be non-negative")
    @DecimalMax(value = "10.0", message = "Lambda South must not exceed 10.0")
    private Double lambdaSouth;
    
    @DecimalMin(value = "0.0", message = "Lambda East must be non-negative")
    @DecimalMax(value = "10.0", message = "Lambda East must not exceed 10.0")
    private Double lambdaEast;
    
    @DecimalMin(value = "0.0", message = "Lambda West must be non-negative")
    @DecimalMax(value = "10.0", message = "Lambda West must not exceed 10.0")
    private Double lambdaWest;
    
    // Pedestrian arrival rates
    @DecimalMin(value = "0.0", message = "Mu North must be non-negative")
    @DecimalMax(value = "5.0", message = "Mu North must not exceed 5.0")
    private Double muNorth;
    
    @DecimalMin(value = "0.0", message = "Mu South must be non-negative")
    @DecimalMax(value = "5.0", message = "Mu South must not exceed 5.0")
    private Double muSouth;
    
    @DecimalMin(value = "0.0", message = "Mu East must be non-negative")
    @DecimalMax(value = "5.0", message = "Mu East must not exceed 5.0")
    private Double muEast;
    
    @DecimalMin(value = "0.0", message = "Mu West must be non-negative")
    @DecimalMax(value = "5.0", message = "Mu West must not exceed 5.0")
    private Double muWest;
    
    // Service rates
    @DecimalMin(value = "0.1", message = "Sigma North must be positive")
    @DecimalMax(value = "10.0", message = "Sigma North must not exceed 10.0")
    private Double sigmaNorth;
    
    @DecimalMin(value = "0.1", message = "Sigma South must be positive")
    @DecimalMax(value = "10.0", message = "Sigma South must not exceed 10.0")
    private Double sigmaSouth;
    
    @DecimalMin(value = "0.1", message = "Sigma East must be positive")
    @DecimalMax(value = "10.0", message = "Sigma East must not exceed 10.0")
    private Double sigmaEast;
    
    @DecimalMin(value = "0.1", message = "Sigma West must be positive")
    @DecimalMax(value = "10.0", message = "Sigma West must not exceed 10.0")
    private Double sigmaWest;
    
    // System parameters
    @Min(value = 5, message = "Minimum green time must be at least 5 seconds")
    @Max(value = 30, message = "Minimum green time must not exceed 30 seconds")
    private Integer minGreenTime;
    
    @Min(value = 30, message = "Maximum green time must be at least 30 seconds")
    @Max(value = 120, message = "Maximum green time must not exceed 120 seconds")
    private Integer maxGreenTime;
    
    @Min(value = 1, message = "Yellow time must be at least 1 second")
    @Max(value = 10, message = "Yellow time must not exceed 10 seconds")
    private Integer yellowTime;
    
    @Min(value = 1, message = "Red clearance time must be at least 1 second")
    @Max(value = 10, message = "Red clearance time must not exceed 10 seconds")
    private Integer redClearanceTime;
    
    @DecimalMin(value = "0.0", message = "Pedestrian weight must be non-negative")
    @DecimalMax(value = "1.0", message = "Pedestrian weight must not exceed 1.0")
    private Double pedestrianWeight;
    
    @DecimalMin(value = "1.0", message = "Switching threshold must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Switching threshold must not exceed 5.0")
    private Double switchingThreshold;
    
    @DecimalMin(value = "0.0", message = "Vehicle performance weight must be non-negative")
    @DecimalMax(value = "1.0", message = "Vehicle performance weight must not exceed 1.0")
    private Double vehiclePerformanceWeight;
    
    @DecimalMin(value = "0.0", message = "Pedestrian performance weight must be non-negative")
    @DecimalMax(value = "1.0", message = "Pedestrian performance weight must not exceed 1.0")
    private Double pedestrianPerformanceWeight;
}