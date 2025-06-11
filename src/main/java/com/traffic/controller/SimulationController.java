package com.traffic.controller;

import com.traffic.dto.*;
import com.traffic.model.*;
import com.traffic.service.TrafficSimulationService;
import com.traffic.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/simulations")
@RequiredArgsConstructor
@Validated
@Slf4j
// @CrossOrigin(origins = "*")
public class SimulationController {
    
    private final TrafficSimulationService simulationService;
    private final SimulationConfigRepository configRepository;
    private final TrafficStateRepository stateRepository;
    private final PerformanceMetricsRepository metricsRepository;
    
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createSimulation(
            @Valid @RequestBody SimulationConfigRequest request) {
        try {
            String simulationId = simulationService.createSimulation(request);
            return ResponseEntity.ok(ApiResponse.success("Simulation created successfully", simulationId));
        } catch (Exception e) {
            log.error("Error creating simulation", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create simulation: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{simulationId}/start")
    public ResponseEntity<ApiResponse<Void>> startSimulation(
            @PathVariable @NotBlank String simulationId,
            @Valid @RequestBody SimulationRequest request) {
        try {
            request.setSimulationId(simulationId);
            simulationService.startSimulation(request);
            return ResponseEntity.ok(ApiResponse.success("Simulation started successfully", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Simulation already running"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid simulation: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error starting simulation", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start simulation: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{simulationId}/stop")
    public ResponseEntity<ApiResponse<Void>> stopSimulation(
            @PathVariable @NotBlank String simulationId) {
        try {
            simulationService.stopSimulation(simulationId);
            return ResponseEntity.ok(ApiResponse.success("Simulation stopped successfully", null));
        } catch (Exception e) {
            log.error("Error stopping simulation", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to stop simulation: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{simulationId}/status")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> getSimulationStatus(
            @PathVariable @NotBlank String simulationId) {
        try {
            SimulationStatusResponse status = simulationService.getSimulationStatus(simulationId);
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting simulation status", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get simulation status"));
        }
    }
    
    @GetMapping("/{simulationId}/config")
    public ResponseEntity<ApiResponse<SimulationConfigResponse>> getSimulationConfig(
            @PathVariable @NotBlank String simulationId) {
        try {
            SimulationConfig config = configRepository.findBySimulationId(simulationId)
                    .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));
            
            SimulationConfigResponse response = convertToConfigResponse(config);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting simulation config", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get simulation config"));
        }
    }
    
    @GetMapping("/{simulationId}/states")
    public ResponseEntity<ApiResponse<List<TrafficStateResponse>>> getTrafficStates(
            @PathVariable @NotBlank String simulationId,
            @RequestParam(required = false) Long fromStep,
            @RequestParam(required = false) Long toStep,
            @RequestParam(defaultValue = "1000") int limit) {
        try {
            List<TrafficState> states;
            
            if (fromStep != null && toStep != null) {
                states = stateRepository.findBySimulationIdAndTimeStepRange(simulationId, fromStep, toStep);
            } else {
                states = stateRepository.findBySimulationIdOrderByTimeStepAsc(simulationId);
            }
            
            // Limit results to prevent memory issues
            if (states.size() > limit) {
                states = states.subList(Math.max(0, states.size() - limit), states.size());
            }
            
            List<TrafficStateResponse> responses = states.stream()
                    .map(this::convertToStateResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            log.error("Error getting traffic states", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get traffic states"));
        }
    }
    
    @GetMapping("/{simulationId}/metrics")
    public ResponseEntity<ApiResponse<PerformanceMetricsResponse>> getPerformanceMetrics(
            @PathVariable @NotBlank String simulationId) {
        try {
            PerformanceMetrics metrics = simulationService.calculatePerformanceMetrics(simulationId);
            PerformanceMetricsResponse response = convertToMetricsResponse(metrics);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid simulation: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error calculating performance metrics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to calculate performance metrics"));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<SimulationConfigResponse>>> getAllSimulations() {
        try {
            List<SimulationConfig> configs = configRepository.findAll();
            List<SimulationConfigResponse> responses = configs.stream()
                    .map(this::convertToConfigResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            log.error("Error getting all simulations", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get simulations"));
        }
    }
    
    @DeleteMapping("/{simulationId}")
    public ResponseEntity<ApiResponse<Void>> deleteSimulation(
            @PathVariable @NotBlank String simulationId) {
        try {
            // Stop simulation if running
            simulationService.stopSimulation(simulationId);
            
            // Delete all related data
            stateRepository.deleteBySimulationId(simulationId);
            metricsRepository.deleteBySimulationId(simulationId);
            configRepository.deleteBySimulationId(simulationId);
            
            return ResponseEntity.ok(ApiResponse.success("Simulation deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting simulation", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete simulation"));
        }
    }
    
    // Conversion methods
    private SimulationConfigResponse convertToConfigResponse(SimulationConfig config) {
        return SimulationConfigResponse.builder()
                .simulationId(config.getSimulationId())
                .name(config.getName())
                .scenario(config.getScenario())
                .lambdaNorth(config.getLambdaNorth())
                .lambdaSouth(config.getLambdaSouth())
                .lambdaEast(config.getLambdaEast())
                .lambdaWest(config.getLambdaWest())
                .muNorth(config.getMuNorth())
                .muSouth(config.getMuSouth())
                .muEast(config.getMuEast())
                .muWest(config.getMuWest())
                .sigmaNorth(config.getSigmaNorth())
                .sigmaSouth(config.getSigmaSouth())
                .sigmaEast(config.getSigmaEast())
                .sigmaWest(config.getSigmaWest())
                .minGreenTime(config.getMinGreenTime())
                .maxGreenTime(config.getMaxGreenTime())
                .yellowTime(config.getYellowTime())
                .redClearanceTime(config.getRedClearanceTime())
                .pedestrianWeight(config.getPedestrianWeight())
                .switchingThreshold(config.getSwitchingThreshold())
                .vehiclePerformanceWeight(config.getVehiclePerformanceWeight())
                .pedestrianPerformanceWeight(config.getPedestrianPerformanceWeight())
                .createdAt(config.getCreatedAt())
                .isActive(config.getIsActive())
                .build();
    }
    
    private TrafficStateResponse convertToStateResponse(TrafficState state) {
        return TrafficStateResponse.builder()
                .timeStep(state.getTimeStep())
                .timestamp(state.getTimestamp())
                .vehiclesNorth(state.getVehiclesNorth())
                .vehiclesSouth(state.getVehiclesSouth())
                .vehiclesEast(state.getVehiclesEast())
                .vehiclesWest(state.getVehiclesWest())
                .pedestriansNorth(state.getPedestriansNorth())
                .pedestriansSouth(state.getPedestriansSouth())
                .pedestriansEast(state.getPedestriansEast())
                .pedestriansWest(state.getPedestriansWest())
                .currentPhase(state.getCurrentPhase())
                .currentGreenTime(state.getCurrentGreenTime())
                .calculatedGreenTime(state.getCalculatedGreenTime())
                .phase1Density(state.getPhase1Density())
                .phase2Density(state.getPhase2Density())
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



