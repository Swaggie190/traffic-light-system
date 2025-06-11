package com.traffic.controller;

import com.traffic.dto.*;
import com.traffic.model.*;
import com.traffic.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    
    private final SimulationConfigRepository configRepository;
    private final PerformanceMetricsRepository metricsRepository;
    private final TrafficStateRepository stateRepository;
    
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboardSummary() {
        try {
            List<SimulationConfig> allConfigs = configRepository.findAll();
            List<PerformanceMetrics> allMetrics = metricsRepository.findAllByOrderByCalculatedAtDesc();
            
            int totalSimulations = allConfigs.size();
            int activeSimulations = configRepository.findByIsActiveTrue().size();
            int completedSimulations = allMetrics.size();
            
            double avgPerformanceIndex = allMetrics.stream()
                    .mapToDouble(PerformanceMetrics::getCombinedPerformanceIndex)
                    .average()
                    .orElse(0.0);
            
            String bestScenario = allMetrics.stream()
                    .min((m1, m2) -> Double.compare(m1.getCombinedPerformanceIndex(), m2.getCombinedPerformanceIndex()))
                    .map(m -> configRepository.findBySimulationId(m.getSimulationId())
                            .map(c -> c.getScenario().getDescription())
                            .orElse("Unknown"))
                    .orElse("None");
            
            SimulationConfigResponse mostRecent = allConfigs.stream()
                    .max((c1, c2) -> c1.getCreatedAt().compareTo(c2.getCreatedAt()))
                    .map(this::convertToConfigResponse)
                    .orElse(null);
            
            List<QuickStats> recentPerformance = allMetrics.stream()
                    .limit(5)
                    .map(this::convertToQuickStats)
                    .collect(Collectors.toList());
            
            DashboardSummary summary = DashboardSummary.builder()
                    .totalSimulations(totalSimulations)
                    .activeSimulations(activeSimulations)
                    .completedSimulations(completedSimulations)
                    .averagePerformanceIndex(avgPerformanceIndex)
                    .bestPerformingScenario(bestScenario)
                    .mostRecentSimulation(mostRecent)
                    .recentPerformance(recentPerformance)
                    .lastUpdate(LocalDateTime.now())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            log.error("Error generating dashboard summary", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate dashboard summary"));
        }
    }
    
    @GetMapping("/traffic-light/{simulationId}")
    public ResponseEntity<ApiResponse<TrafficLightStatus>> getTrafficLightStatus(
            @PathVariable String simulationId) {
        try {
            TrafficState latestState = stateRepository.findLatestBySimulationId(simulationId)
                    .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));
            
            // Calculate light states based on current phase
            LightState northSouth, eastWest;
            if (latestState.getCurrentPhase() == TrafficPhase.PHASE_1) {
                // N-S Green, E-W Red
                northSouth = LightState.builder()
                        .color("GREEN")
                        .duration(latestState.getCalculatedGreenTime() - latestState.getCurrentGreenTime())
                        .pedestrianCrossing(false)
                        .build();
                eastWest = LightState.builder()
                        .color("RED")
                        .duration(latestState.getCalculatedGreenTime() - latestState.getCurrentGreenTime())
                        .pedestrianCrossing(true)
                        .build();
            } else {
                // E-W Green, N-S Red
                northSouth = LightState.builder()
                        .color("RED")
                        .duration(latestState.getCalculatedGreenTime() - latestState.getCurrentGreenTime())
                        .pedestrianCrossing(true)
                        .build();
                eastWest = LightState.builder()
                        .color("GREEN")
                        .duration(latestState.getCalculatedGreenTime() - latestState.getCurrentGreenTime())
                        .pedestrianCrossing(false)
                        .build();
            }
            
            TrafficLightStatus status = TrafficLightStatus.builder()
                    .simulationId(simulationId)
                    .timeStep(latestState.getTimeStep())
                    .timestamp(latestState.getTimestamp())
                    .northSouth(northSouth)
                    .eastWest(eastWest)
                    .currentGreenTime(latestState.getCurrentGreenTime())
                    .remainingGreenTime(latestState.getCalculatedGreenTime() - latestState.getCurrentGreenTime())
                    .nextPhaseCountdown(Math.max(0, latestState.getCalculatedGreenTime() - latestState.getCurrentGreenTime()))
                    .northSouthDensity(latestState.getPhase1Density())
                    .eastWestDensity(latestState.getPhase2Density())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting traffic light status", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get traffic light status"));
        }
    }
    
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<ComparisonReport>> compareSimulations(
            @RequestBody List<String> simulationIds) {
        try {
            if (simulationIds.size() < 2) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("At least 2 simulations required for comparison"));
            }
            
            List<PerformanceMetrics> metrics = simulationIds.stream()
                    .map(id -> metricsRepository.findBySimulationId(id).orElse(null))
                    .filter(m -> m != null)
                    .collect(Collectors.toList());
            
            if (metrics.size() < 2) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Not enough simulation data found for comparison"));
            }
            
            // Find baseline (worst performing) for improvement calculation
            double baselineIndex = metrics.stream()
                    .mapToDouble(PerformanceMetrics::getCombinedPerformanceIndex)
                    .max()
                    .orElse(0.0);
            
            List<SimulationComparison> comparisons = metrics.stream()
                    .map(m -> {
                        SimulationConfig config = configRepository.findBySimulationId(m.getSimulationId()).orElse(null);
                        double improvement = baselineIndex > 0 ? 
                                ((baselineIndex - m.getCombinedPerformanceIndex()) / baselineIndex) * 100 : 0;
                        
                        return SimulationComparison.builder()
                                .simulationId(m.getSimulationId())
                                .name(config != null ? config.getName() : "Unknown")
                                .scenario(config != null ? config.getScenario() : null)
                                .vehicleWaitingTime(m.getAverageVehicleWaitingTime())
                                .pedestrianWaitingTime(m.getAveragePedestrianWaitingTime())
                                .combinedIndex(m.getCombinedPerformanceIndex())
                                .improvementPercent(improvement)
                                .build();
                    })
                    .sorted((c1, c2) -> Double.compare(c1.getCombinedIndex(), c2.getCombinedIndex()))
                    .collect(Collectors.toList());
            
            // Assign ranks
            for (int i = 0; i < comparisons.size(); i++) {
                comparisons.get(i).setRank(i + 1);
            }
            
            // Generate summary
            SimulationComparison best = comparisons.get(0);
            SimulationComparison worst = comparisons.get(comparisons.size() - 1);
            double avgImprovement = comparisons.stream()
                    .mapToDouble(SimulationComparison::getImprovementPercent)
                    .average()
                    .orElse(0.0);
            
            ComparisonSummary summary = ComparisonSummary.builder()
                    .bestSimulation(best.getName())
                    .worstSimulation(worst.getName())
                    .averageImprovement(avgImprovement)
                    .recommendedScenario(best.getScenario() != null ? best.getScenario().getDescription() : "Unknown")
                    .insights(generateInsights(comparisons))
                    .build();
            
            ComparisonReport report = ComparisonReport.builder()
                    .simulations(comparisons)
                    .summary(summary)
                    .generatedAt(LocalDateTime.now())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            log.error("Error generating comparison report", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate comparison report"));
        }
    }
    
    @GetMapping("/top-performers")
    public ResponseEntity<ApiResponse<List<PerformanceMetricsResponse>>> getTopPerformers(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<PerformanceMetrics> topPerformers = metricsRepository.findTopPerformingSimulations(limit);
            
            List<PerformanceMetricsResponse> responses = topPerformers.stream()
                    .map(this::convertToMetricsResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            log.error("Error getting top performers", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get top performers"));
        }
    }
    
    private String generateInsights(List<SimulationComparison> comparisons) {
        if (comparisons.size() < 2) return "Insufficient data for insights";
        
        SimulationComparison best = comparisons.get(0);
        SimulationComparison worst = comparisons.get(comparisons.size() - 1);
        
        double improvementGap = worst.getCombinedIndex() - best.getCombinedIndex();
        
        StringBuilder insights = new StringBuilder();
        insights.append(String.format("Best performing scenario '%s' outperforms worst by %.2f%%. ", 
                best.getScenario().getDescription(), 
                (improvementGap / worst.getCombinedIndex()) * 100));
        
        if (best.getVehicleWaitingTime() < worst.getVehicleWaitingTime()) {
            insights.append("Vehicle waiting time optimization shows significant impact. ");
        }
        
        if (best.getPedestrianWaitingTime() < worst.getPedestrianWaitingTime()) {
            insights.append("Pedestrian flow management contributes to better performance. ");
        }
        
        return insights.toString();
    }
    
    // Helper conversion methods
    private SimulationConfigResponse convertToConfigResponse(SimulationConfig config) {
        return SimulationConfigResponse.builder()
                .simulationId(config.getSimulationId())
                .name(config.getName())
                .scenario(config.getScenario())
                .createdAt(config.getCreatedAt())
                .isActive(config.getIsActive())
                .build();
    }
    
    private QuickStats convertToQuickStats(PerformanceMetrics metrics) {
        SimulationConfig config = configRepository.findBySimulationId(metrics.getSimulationId()).orElse(null);
        return QuickStats.builder()
                .simulationId(metrics.getSimulationId())
                .name(config != null ? config.getName() : "Unknown")
                .performanceIndex(metrics.getCombinedPerformanceIndex())
                .scenario(config != null ? config.getScenario() : null)
                .completedAt(metrics.getCalculatedAt())
                .build();
    }
    
    private PerformanceMetricsResponse convertToMetricsResponse(PerformanceMetrics metrics) {
        return PerformanceMetricsResponse.builder()
                .simulationId(metrics.getSimulationId())
                .totalTimeSteps(metrics.getTotalTimeSteps())
                .averageVehicleWaitingTime(metrics.getAverageVehicleWaitingTime())
                .averagePedestrianWaitingTime(metrics.getAveragePedestrianWaitingTime())
                .combinedPerformanceIndex(metrics.getCombinedPerformanceIndex())
                .totalVehiclesProcessed(metrics.getTotalVehiclesProcessed())
                .totalPedestriansProcessed(metrics.getTotalPedestriansProcessed())
                .phase1TotalTime(metrics.getPhase1TotalTime())
                .phase2TotalTime(metrics.getPhase2TotalTime())
                .calculatedAt(metrics.getCalculatedAt())
                .build();
    }
}