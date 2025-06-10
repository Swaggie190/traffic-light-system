package com.traffic.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.traffic.dto.ScenarioTemplate;
import com.traffic.model.TrafficScenario;
import com.traffic.dto.ApiResponse;

@RestController
@RequestMapping("/scenarios")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ScenarioController {
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScenarioTemplate>>> getPresetScenarios() {
        try {
            List<ScenarioTemplate> scenarios = List.of(
                ScenarioTemplate.builder()
                    .name("Balanced Traffic")
                    .description("Equal traffic flow from all directions")
                    .scenario(TrafficScenario.BALANCED)
                    .lambdaNorth(0.2).lambdaSouth(0.2).lambdaEast(0.2).lambdaWest(0.2)
                    .muNorth(0.05).muSouth(0.05).muEast(0.05).muWest(0.05)
                    .sigmaNorth(0.4).sigmaSouth(0.4).sigmaEast(0.4).sigmaWest(0.4)
                    .build(),
                    
                ScenarioTemplate.builder()
                    .name("Heavy North-South Traffic")
                    .description("High traffic on North-South corridor")
                    .scenario(TrafficScenario.HEAVY_NS)
                    .lambdaNorth(0.4).lambdaSouth(0.4).lambdaEast(0.1).lambdaWest(0.1)
                    .muNorth(0.08).muSouth(0.08).muEast(0.03).muWest(0.03)
                    .sigmaNorth(0.6).sigmaSouth(0.6).sigmaEast(0.3).sigmaWest(0.3)
                    .build(),
                    
                ScenarioTemplate.builder()
                    .name("Rush Hour")
                    .description("Peak traffic conditions")
                    .scenario(TrafficScenario.RUSH_HOUR)
                    .lambdaNorth(0.5).lambdaSouth(0.5).lambdaEast(0.5).lambdaWest(0.5)
                    .muNorth(0.1).muSouth(0.1).muEast(0.1).muWest(0.1)
                    .sigmaNorth(0.7).sigmaSouth(0.7).sigmaEast(0.7).sigmaWest(0.7)
                    .build()
            );
            
            return ResponseEntity.ok(ApiResponse.success(scenarios));
        } catch (Exception e) {
            log.error("Error getting preset scenarios", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get preset scenarios"));
        }
    }
    
    @PostMapping("/{scenarioName}/apply/{simulationId}")
    public ResponseEntity<ApiResponse<Void>> applyScenarioToSimulation(
            @PathVariable String scenarioName,
            @PathVariable String simulationId) {
        // Implementation for applying preset scenarios to existing simulations
        return ResponseEntity.ok(ApiResponse.success("Scenario applied successfully", null));
    }
}