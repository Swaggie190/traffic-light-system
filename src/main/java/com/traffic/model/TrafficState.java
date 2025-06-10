package com.traffic.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "simulation_id")
    private String simulationId;
    
    @Column(name = "time_step")
    private Long timeStep;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    // Vehicle queue lengths
    @Column(name = "vehicles_north")
    private Integer vehiclesNorth;
    
    @Column(name = "vehicles_south")
    private Integer vehiclesSouth;
    
    @Column(name = "vehicles_east")
    private Integer vehiclesEast;
    
    @Column(name = "vehicles_west")
    private Integer vehiclesWest;
    
    // Pedestrian waiting counts
    @Column(name = "pedestrians_north")
    private Integer pedestriansNorth;
    
    @Column(name = "pedestrians_south")
    private Integer pedestriansSouth;
    
    @Column(name = "pedestrians_east")
    private Integer pedestriansEast;
    
    @Column(name = "pedestrians_west")
    private Integer pedestriansWest;
    
    // Current traffic phase
    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase")
    private TrafficPhase currentPhase;
    
    // Green time for current phase
    @Column(name = "current_green_time")
    private Integer currentGreenTime;
    
    // Calculated green time for this phase
    @Column(name = "calculated_green_time")
    private Integer calculatedGreenTime;
    
    // Phase densities
    @Column(name = "phase1_density")
    private Double phase1Density;
    
    @Column(name = "phase2_density")
    private Double phase2Density;
}