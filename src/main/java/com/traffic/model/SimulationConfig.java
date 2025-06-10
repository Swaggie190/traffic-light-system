package com.traffic.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "simulation_id", unique = true)
    private String simulationId;
    
    @Column(name = "name")
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "scenario")
    private TrafficScenario scenario;
    
    // Vehicle arrival rates (vehicles/second)
    @Column(name = "lambda_north")
    private Double lambdaNorth;
    
    @Column(name = "lambda_south")
    private Double lambdaSouth;
    
    @Column(name = "lambda_east")
    private Double lambdaEast;
    
    @Column(name = "lambda_west")
    private Double lambdaWest;
    
    // Pedestrian arrival rates (pedestrians/second)
    @Column(name = "mu_north")
    private Double muNorth;
    
    @Column(name = "mu_south")
    private Double muSouth;
    
    @Column(name = "mu_east")
    private Double muEast;
    
    @Column(name = "mu_west")
    private Double muWest;
    
    // Service rates (vehicles/second when green)
    @Column(name = "sigma_north")
    private Double sigmaNorth;
    
    @Column(name = "sigma_south")
    private Double sigmaSouth;
    
    @Column(name = "sigma_east")
    private Double sigmaEast;
    
    @Column(name = "sigma_west")
    private Double sigmaWest;
    
    // System parameters
    @Column(name = "min_green_time")
    private Integer minGreenTime;
    
    @Column(name = "max_green_time")
    private Integer maxGreenTime;
    
    @Column(name = "yellow_time")
    private Integer yellowTime;
    
    @Column(name = "red_clearance_time")
    private Integer redClearanceTime;
    
    @Column(name = "pedestrian_weight")
    private Double pedestrianWeight;
    
    @Column(name = "switching_threshold")
    private Double switchingThreshold;
    
    @Column(name = "vehicle_performance_weight")
    private Double vehiclePerformanceWeight;
    
    @Column(name = "pedestrian_performance_weight")
    private Double pedestrianPerformanceWeight;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "is_active")
    private Boolean isActive;
}
