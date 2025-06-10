package com.traffic.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "simulation_id")
    private String simulationId;
    
    @Column(name = "total_time_steps")
    private Long totalTimeSteps;
    
    @Column(name = "average_vehicle_waiting_time")
    private Double averageVehicleWaitingTime;
    
    @Column(name = "average_pedestrian_waiting_time")
    private Double averagePedestrianWaitingTime;
    
    @Column(name = "combined_performance_index")
    private Double combinedPerformanceIndex;
    
    @Column(name = "total_vehicles_processed")
    private Long totalVehiclesProcessed;
    
    @Column(name = "total_pedestrians_processed")
    private Long totalPedestriansProcessed;
    
    @Column(name = "phase1_total_time")
    private Long phase1TotalTime;
    
    @Column(name = "phase2_total_time")
    private Long phase2TotalTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}