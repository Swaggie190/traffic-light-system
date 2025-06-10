package com.traffic.service;

import com.traffic.model.*;
import com.traffic.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.traffic.dto.SimulationRequest;
import com.traffic.dto.SimulationConfigRequest;
import com.traffic.dto.SimulationStatusResponse;
import com.traffic.dto.TrafficStateResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficSimulationService {
    
    private final TrafficStateRepository trafficStateRepository;
    private final SimulationConfigRepository simulationConfigRepository;
    private final PerformanceMetricsRepository performanceMetricsRepository;
    private final WebSocketService webSocketService;
    
    private final Map<String, SimulationRunner> activeSimulations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    
    // Mathematical model implementation
    public TrafficState calculateNextState(TrafficState currentState, SimulationConfig config) {
        TrafficState nextState = TrafficState.builder()
                .simulationId(currentState.getSimulationId())
                .timeStep(currentState.getTimeStep() + 1)
                .timestamp(LocalDateTime.now())
                .currentPhase(currentState.getCurrentPhase())
                .currentGreenTime(currentState.getCurrentGreenTime() + 1)
                .build();
        
        // Update vehicle queues based on current phase
        if (currentState.getCurrentPhase() == TrafficPhase.PHASE_1) {
            // N-S green, E-W red
            nextState.setVehiclesNorth(Math.max(0, 
                currentState.getVehiclesNorth() + config.getLambdaNorth().intValue() - config.getSigmaNorth().intValue()));
            nextState.setVehiclesSouth(Math.max(0, 
                currentState.getVehiclesSouth() + config.getLambdaSouth().intValue() - config.getSigmaSouth().intValue()));
            nextState.setVehiclesEast(currentState.getVehiclesEast() + config.getLambdaEast().intValue());
            nextState.setVehiclesWest(currentState.getVehiclesWest() + config.getLambdaWest().intValue());
            
            // Pedestrians can cross E-W during N-S green
            nextState.setPedestriansNorth(currentState.getPedestriansNorth() + config.getMuNorth().intValue());
            nextState.setPedestriansSouth(currentState.getPedestriansSouth() + config.getMuSouth().intValue());
            nextState.setPedestriansEast(Math.max(0, currentState.getPedestriansEast() - currentState.getPedestriansEast()));
            nextState.setPedestriansWest(Math.max(0, currentState.getPedestriansWest() - currentState.getPedestriansWest()));
        } else {
            // E-W green, N-S red
            nextState.setVehiclesNorth((int)(currentState.getVehiclesNorth() + config.getLambdaNorth()));
            nextState.setVehiclesSouth((int)(currentState.getVehiclesSouth() + config.getLambdaSouth()));
            nextState.setVehiclesEast(Math.max(0, 
                currentState.getVehiclesEast() + config.getLambdaEast().intValue() - config.getSigmaEast().intValue()));
            nextState.setVehiclesWest(Math.max(0, 
                currentState.getVehiclesWest() + config.getLambdaWest().intValue() - config.getSigmaWest().intValue()));
            
            // Pedestrians can cross N-S during E-W green
            nextState.setPedestriansNorth(Math.max(0, currentState.getPedestriansNorth() - currentState.getPedestriansNorth()));
            nextState.setPedestriansSouth(Math.max(0, currentState.getPedestriansSouth() - currentState.getPedestriansSouth()));
            nextState.setPedestriansEast(currentState.getPedestriansEast() + config.getMuEast().intValue());
            nextState.setPedestriansWest(currentState.getPedestriansWest() + config.getMuWest().intValue());
        }
        
        // Calculate traffic densities
        double phase1Density = nextState.getVehiclesNorth() + nextState.getVehiclesSouth() + 
                              config.getPedestrianWeight() * (nextState.getPedestriansNorth() + nextState.getPedestriansSouth());
        double phase2Density = nextState.getVehiclesEast() + nextState.getVehiclesWest() + 
                              config.getPedestrianWeight() * (nextState.getPedestriansEast() + nextState.getPedestriansWest());
        
        nextState.setPhase1Density(phase1Density);
        nextState.setPhase2Density(phase2Density);
        
        // Calculate adaptive green time
        double currentDensity = (currentState.getCurrentPhase() == TrafficPhase.PHASE_1) ? phase1Density : phase2Density;
        double waitingDensity = (currentState.getCurrentPhase() == TrafficPhase.PHASE_1) ? phase2Density : phase1Density;
        
        int adaptiveGreenTime = config.getMinGreenTime() + 
            (int)((config.getMaxGreenTime() - config.getMinGreenTime()) * 
                  (currentDensity / (currentDensity + waitingDensity + 1.0)));
        
        nextState.setCalculatedGreenTime(adaptiveGreenTime);
        
        // Check for phase switching
        boolean shouldSwitch = shouldSwitchPhase(nextState, config);
        if (shouldSwitch) {
            nextState.setCurrentPhase(currentState.getCurrentPhase() == TrafficPhase.PHASE_1 ? 
                                    TrafficPhase.PHASE_2 : TrafficPhase.PHASE_1);
            nextState.setCurrentGreenTime(0);
        }
        
        return nextState;
    }
    
    private boolean shouldSwitchPhase(TrafficState state, SimulationConfig config) {
        if (state.getCurrentGreenTime() < state.getCalculatedGreenTime()) {
            return false;
        }
        
        double currentDensity = (state.getCurrentPhase() == TrafficPhase.PHASE_1) ? 
                               state.getPhase1Density() : state.getPhase2Density();
        double waitingDensity = (state.getCurrentPhase() == TrafficPhase.PHASE_1) ? 
                               state.getPhase2Density() : state.getPhase1Density();
        
        return waitingDensity > config.getSwitchingThreshold() * currentDensity || 
               state.getCurrentGreenTime() >= config.getMaxGreenTime();
    }
    
    @Transactional
    public String createSimulation(SimulationConfigRequest request) {
        String simulationId = UUID.randomUUID().toString();
        
        SimulationConfig config = SimulationConfig.builder()
                .simulationId(simulationId)
                .name(request.getName())
                .scenario(request.getScenario())
                .lambdaNorth(request.getLambdaNorth())
                .lambdaSouth(request.getLambdaSouth())
                .lambdaEast(request.getLambdaEast())
                .lambdaWest(request.getLambdaWest())
                .muNorth(request.getMuNorth())
                .muSouth(request.getMuSouth())
                .muEast(request.getMuEast())
                .muWest(request.getMuWest())
                .sigmaNorth(request.getSigmaNorth())
                .sigmaSouth(request.getSigmaSouth())
                .sigmaEast(request.getSigmaEast())
                .sigmaWest(request.getSigmaWest())
                .minGreenTime(request.getMinGreenTime())
                .maxGreenTime(request.getMaxGreenTime())
                .yellowTime(request.getYellowTime())
                .redClearanceTime(request.getRedClearanceTime())
                .pedestrianWeight(request.getPedestrianWeight())
                .switchingThreshold(request.getSwitchingThreshold())
                .vehiclePerformanceWeight(request.getVehiclePerformanceWeight())
                .pedestrianPerformanceWeight(request.getPedestrianPerformanceWeight())
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        
        simulationConfigRepository.save(config);
        
        // Create initial state
        TrafficState initialState = TrafficState.builder()
                .simulationId(simulationId)
                .timeStep(0L)
                .timestamp(LocalDateTime.now())
                .vehiclesNorth(0)
                .vehiclesSouth(0)
                .vehiclesEast(0)
                .vehiclesWest(0)
                .pedestriansNorth(0)
                .pedestriansSouth(0)
                .pedestriansEast(0)
                .pedestriansWest(0)
                .currentPhase(TrafficPhase.PHASE_1)
                .currentGreenTime(0)
                .calculatedGreenTime(config.getMinGreenTime())
                .phase1Density(0.0)
                .phase2Density(0.0)
                .build();
        
        trafficStateRepository.save(initialState);
        
        log.info("Created simulation: {}", simulationId);
        return simulationId;
    }
    
    public void startSimulation(SimulationRequest request) {
        String simulationId = request.getSimulationId();
        
        if (activeSimulations.containsKey(simulationId)) {
            throw new IllegalStateException("Simulation is already running");
        }
        
        SimulationConfig config = simulationConfigRepository.findBySimulationId(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));
        
        SimulationRunner runner = new SimulationRunner(simulationId, config, request);
        activeSimulations.put(simulationId, runner);
        
        runner.start();
        log.info("Started simulation: {}", simulationId);
    }
    
    public void stopSimulation(String simulationId) {
        SimulationRunner runner = activeSimulations.remove(simulationId);
        if (runner != null) {
            runner.stop();
            log.info("Stopped simulation: {}", simulationId);
        }
    }
    
    public SimulationStatusResponse getSimulationStatus(String simulationId) {
        SimulationRunner runner = activeSimulations.get(simulationId);
        
        if (runner != null) {
            return runner.getStatus();
        }
        
        // Check if simulation exists but is not running
        boolean exists = simulationConfigRepository.findBySimulationId(simulationId).isPresent();
        if (!exists) {
            throw new IllegalArgumentException("Simulation not found");
        }
        
        TrafficState latestState = trafficStateRepository.findLatestBySimulationId(simulationId).orElse(null);
        
        return SimulationStatusResponse.builder()
                .simulationId(simulationId)
                .status("IDLE")
                .currentTimeStep(latestState != null ? latestState.getTimeStep() : 0L)
                .progress(0.0)
                .lastUpdate(LocalDateTime.now())
                .build();
    }
    
    public PerformanceMetrics calculatePerformanceMetrics(String simulationId) {
        List<TrafficState> states = trafficStateRepository.findBySimulationIdOrderByTimeStepAsc(simulationId);
        
        if (states.isEmpty()) {
            throw new IllegalArgumentException("No simulation data found");
        }
        
        SimulationConfig config = simulationConfigRepository.findBySimulationId(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation config not found"));
        
        long totalTimeSteps = states.size();
        
        // Calculate average waiting times
        double totalVehicleWaiting = states.stream()
                .mapToDouble(s -> s.getVehiclesNorth() + s.getVehiclesSouth() + 
                               s.getVehiclesEast() + s.getVehiclesWest())
                .sum();
        
        double totalPedestrianWaiting = states.stream()
                .mapToDouble(s -> s.getPedestriansNorth() + s.getPedestriansSouth() + 
                                s.getPedestriansEast() + s.getPedestriansWest())
                .sum();
        
        double avgVehicleWaiting = totalVehicleWaiting / totalTimeSteps;
        double avgPedestrianWaiting = totalPedestrianWaiting / totalTimeSteps;
        
        // Calculate combined performance index
        double combinedIndex = config.getVehiclePerformanceWeight() * avgVehicleWaiting + 
                              config.getPedestrianPerformanceWeight() * avgPedestrianWaiting;
        
        // Calculate phase times
        long phase1Time = states.stream()
                .mapToLong(s -> s.getCurrentPhase() == TrafficPhase.PHASE_1 ? 1 : 0)
                .sum();
        long phase2Time = totalTimeSteps - phase1Time;
        
        PerformanceMetrics metrics = PerformanceMetrics.builder()
                .simulationId(simulationId)
                .totalTimeSteps(totalTimeSteps)
                .averageVehicleWaitingTime(avgVehicleWaiting)
                .averagePedestrianWaitingTime(avgPedestrianWaiting)
                .combinedPerformanceIndex(combinedIndex)
                .totalVehiclesProcessed(0L) // To be calculated based on service rates
                .totalPedestriansProcessed(0L) // To be calculated
                .phase1TotalTime(phase1Time)
                .phase2TotalTime(phase2Time)
                .calculatedAt(LocalDateTime.now())
                .build();
        
        return performanceMetricsRepository.save(metrics);
    }
    
    private class SimulationRunner {
        private final String simulationId;
        private final SimulationConfig config;
        private final SimulationRequest request;
        private volatile boolean running = false;
        private volatile String status = "IDLE";
        private volatile long currentTimeStep = 0;
        private volatile String errorMessage;
        
        public SimulationRunner(String simulationId, SimulationConfig config, SimulationRequest request) {
            this.simulationId = simulationId;
            this.config = config;
            this.request = request;
        }
        
        public void start() {
            if (running) return;
            
            running = true;
            status = "RUNNING";
            
            TrafficState currentState = trafficStateRepository.findLatestBySimulationId(simulationId)
                    .orElseThrow(() -> new IllegalArgumentException("No initial state found"));
            
            long totalSteps = request.getDurationSeconds();
            int stepInterval = request.getTimeStepMillis();
            
            executorService.scheduleAtFixedRate(() -> {
                try {
                    if (!running || currentTimeStep >= totalSteps) {
                        stop();
                        return;
                    }
                    
                    TrafficState nextState = calculateNextState(currentState, config);
                    trafficStateRepository.save(nextState);
                    
                    currentTimeStep = nextState.getTimeStep();
                    
                    // Send real-time update via WebSocket
                    webSocketService.sendTrafficUpdate(simulationId, convertToResponse(nextState));
                    
                } catch (Exception e) {
                    log.error("Error in simulation step: ", e);
                    errorMessage = e.getMessage();
                    status = "ERROR";
                    stop();
                }
            }, 0, stepInterval, TimeUnit.MILLISECONDS);
        }
        
        public void stop() {
            running = false;
            if ("ERROR".equals(status)) {
                status = "ERROR";
            } else if (currentTimeStep >= request.getDurationSeconds()) {
                status = "COMPLETED";
            } else {
                status = "STOPPED";
            }
            
            activeSimulations.remove(simulationId);
        }
        
        public SimulationStatusResponse getStatus() {
            double progress = request.getDurationSeconds() > 0 ? 
                            (double) currentTimeStep / request.getDurationSeconds() * 100.0 : 0.0;
            
            return SimulationStatusResponse.builder()
                    .simulationId(simulationId)
                    .status(status)
                    .currentTimeStep(currentTimeStep)
                    .totalTimeSteps((long) request.getDurationSeconds())
                    .progress(progress)
                    .message(errorMessage)
                    .lastUpdate(LocalDateTime.now())
                    .build();
        }
    }
    
    private TrafficStateResponse convertToResponse(TrafficState state) {
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
}