package com.traffic.controller;

import com.traffic.dto.*;
import com.traffic.model.*;
import com.traffic.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/sensors")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SensorController {
    
    private final TrafficStateRepository stateRepository;
    
    @PostMapping("/{simulationId}/data")
    public ResponseEntity<ApiResponse<Void>> updateSensorData(
            @PathVariable @NotBlank String simulationId,
            @Valid @RequestBody SensorDataRequest request) {
        try {
            // This endpoint simulates receiving real sensor data
            // In a real system, this would be called by the sensor hardware/software
            
            TrafficState latestState = stateRepository.findLatestBySimulationId(simulationId)
                    .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));
            
            // Update the current state with sensor data
            latestState.setVehiclesNorth(request.getVehiclesNorth());
            latestState.setVehiclesSouth(request.getVehiclesSouth());
            latestState.setVehiclesEast(request.getVehiclesEast());
            latestState.setVehiclesWest(request.getVehiclesWest());
            latestState.setPedestriansNorth(request.getPedestriansNorth());
            latestState.setPedestriansSouth(request.getPedestriansSouth());
            latestState.setPedestriansEast(request.getPedestriansEast());
            latestState.setPedestriansWest(request.getPedestriansWest());
            
            stateRepository.save(latestState);
            
            return ResponseEntity.ok(ApiResponse.success("Sensor data updated successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid simulation: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating sensor data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update sensor data"));
        }
    }
    
    @GetMapping("/{simulationId}/status")
    public ResponseEntity<ApiResponse<SensorStatusResponse>> getSensorStatus(
            @PathVariable @NotBlank String simulationId) {
        try {
            TrafficState latestState = stateRepository.findLatestBySimulationId(simulationId)
                    .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));
            
            SensorStatusResponse response = SensorStatusResponse.builder()
                    .simulationId(simulationId)
                    .inductiveLoopStatus("ACTIVE")
                    .videoDetectionStatus("ACTIVE")
                    .lastUpdate(latestState.getTimestamp())
                    .vehicleDetections(SensorDetection.builder()
                            .north(latestState.getVehiclesNorth())
                            .south(latestState.getVehiclesSouth())
                            .east(latestState.getVehiclesEast())
                            .west(latestState.getVehiclesWest())
                            .build())
                    .pedestrianDetections(SensorDetection.builder()
                            .north(latestState.getPedestriansNorth())
                            .south(latestState.getPedestriansSouth())
                            .east(latestState.getPedestriansEast())
                            .west(latestState.getPedestriansWest())
                            .build())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting sensor status", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get sensor status"));
        }
    }
}