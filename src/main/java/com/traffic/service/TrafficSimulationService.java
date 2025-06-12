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
import java.util.concurrent.ScheduledFuture;
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
    
    // Fixed mathematical model implementation
    public TrafficState calculateNextState(TrafficState currentState, SimulationConfig config) {
        TrafficState nextState = TrafficState.builder()
                .simulationId(currentState.getSimulationId())
                .timeStep(currentState.getTimeStep() + 1)
                .timestamp(LocalDateTime.now())
                .currentPhase(currentState.getCurrentPhase())
                .currentGreenTime(currentState.getCurrentGreenTime() + 1)
                .build();
        
        // Initialize with current values
        nextState.setVehiclesNorth(currentState.getVehiclesNorth());
        nextState.setVehiclesSouth(currentState.getVehiclesSouth());
        nextState.setVehiclesEast(currentState.getVehiclesEast());
        nextState.setVehiclesWest(currentState.getVehiclesWest());
        
        nextState.setPedestriansNorth(currentState.getPedestriansNorth());
        nextState.setPedestriansSouth(currentState.getPedestriansSouth());
        nextState.setPedestriansEast(currentState.getPedestriansEast());
        nextState.setPedestriansWest(currentState.getPedestriansWest());
        
        // Apply arrival and service rates based on current phase
        if (currentState.getCurrentPhase() == TrafficPhase.PHASE_1) {
            // N-S green, E-W red
            log.debug("PHASE_1: N-S green, E-W red");
            
            // N-S directions: vehicles can be served (flow through)
            nextState.setVehiclesNorth(Math.max(0, 
                nextState.getVehiclesNorth() + generateArrivals(config.getLambdaNorth()) - generateService(config.getSigmaNorth())));
            nextState.setVehiclesSouth(Math.max(0, 
                nextState.getVehiclesSouth() + generateArrivals(config.getLambdaSouth()) - generateService(config.getSigmaSouth())));
            
            // E-W directions: vehicles accumulate (red light)
            nextState.setVehiclesEast(nextState.getVehiclesEast() + generateArrivals(config.getLambdaEast()));
            nextState.setVehiclesWest(nextState.getVehiclesWest() + generateArrivals(config.getLambdaWest()));
            
            // Pedestrians: N-S accumulate, E-W can cross
            nextState.setPedestriansNorth(nextState.getPedestriansNorth() + generateArrivals(config.getMuNorth()));
            nextState.setPedestriansSouth(nextState.getPedestriansSouth() + generateArrivals(config.getMuSouth()));
            nextState.setPedestriansEast(Math.max(0, nextState.getPedestriansEast() - nextState.getPedestriansEast())); // All cross
            nextState.setPedestriansWest(Math.max(0, nextState.getPedestriansWest() - nextState.getPedestriansWest())); // All cross
            
        } else {
            // E-W green, N-S red
            log.debug("PHASE_2: E-W green, N-S red");
            
            // N-S directions: vehicles accumulate (red light)
            nextState.setVehiclesNorth(nextState.getVehiclesNorth() + generateArrivals(config.getLambdaNorth()));
            nextState.setVehiclesSouth(nextState.getVehiclesSouth() + generateArrivals(config.getLambdaSouth()));
            
            // E-W directions: vehicles can be served (flow through)
            nextState.setVehiclesEast(Math.max(0, 
                nextState.getVehiclesEast() + generateArrivals(config.getLambdaEast()) - generateService(config.getSigmaEast())));
            nextState.setVehiclesWest(Math.max(0, 
                nextState.getVehiclesWest() + generateArrivals(config.getLambdaWest()) - generateService(config.getSigmaWest())));
            
            // Pedestrians: E-W accumulate, N-S can cross
            nextState.setPedestriansEast(nextState.getPedestriansEast() + generateArrivals(config.getMuEast()));
            nextState.setPedestriansWest(nextState.getPedestriansWest() + generateArrivals(config.getMuWest()));
            nextState.setPedestriansNorth(Math.max(0, nextState.getPedestriansNorth() - nextState.getPedestriansNorth())); // All cross
            nextState.setPedestriansSouth(Math.max(0, nextState.getPedestriansSouth() - nextState.getPedestriansSouth())); // All cross
        }
        
        // Calculate traffic densities
        double phase1Density = nextState.getVehiclesNorth() + nextState.getVehiclesSouth() + 
                              config.getPedestrianWeight() * (nextState.getPedestriansNorth() + nextState.getPedestriansSouth());
        double phase2Density = nextState.getVehiclesEast() + nextState.getVehiclesWest() + 
                              config.getPedestrianWeight() * (nextState.getPedestriansEast() + nextState.getPedestriansWest());
        
        nextState.setPhase1Density(phase1Density);
        nextState.setPhase2Density(phase2Density);
        
        // Calculate adaptive green time
        int adaptiveGreenTime = calculateAdaptiveGreenTime(nextState, config);
        nextState.setCalculatedGreenTime(adaptiveGreenTime);
        
        // Check for phase switching
        boolean shouldSwitch = shouldSwitchPhase(nextState, config);
        if (shouldSwitch) {
            nextState.setCurrentPhase(currentState.getCurrentPhase() == TrafficPhase.PHASE_1 ? 
                                    TrafficPhase.PHASE_2 : TrafficPhase.PHASE_1);
            nextState.setCurrentGreenTime(0);
            
            // Recalculate green time for new phase
            adaptiveGreenTime = calculateAdaptiveGreenTime(nextState, config);
            nextState.setCalculatedGreenTime(adaptiveGreenTime);
            
            log.info("Phase switched to {} - N-S density: {}, E-W density: {}, Green time: {}s", 
                    nextState.getCurrentPhase(), phase1Density, phase2Density, adaptiveGreenTime);
        }
        
        log.debug("State updated: timeStep={}, N={}, S={}, E={}, W={}, phase={}, greenTime={}/{}", 
                nextState.getTimeStep(), nextState.getVehiclesNorth(), nextState.getVehiclesSouth(),
                nextState.getVehiclesEast(), nextState.getVehiclesWest(), 
                nextState.getCurrentPhase(), nextState.getCurrentGreenTime(), nextState.getCalculatedGreenTime());
        
        return nextState;
    }
    
    // Helper method to generate arrivals based on Poisson process
    private int generateArrivals(Double rate) {
        if (rate == null || rate <= 0) return 0;
        
        // Simple arrival generation: rate represents vehicles/second
        // For 1-second time steps, we use Poisson distribution approximation
        double random = Math.random();
        return (random < rate) ? 1 : 0;
    }
    
    // Helper method to generate service (vehicles processed)
    private int generateService(Double rate) {
        if (rate == null || rate <= 0) return 0;
        
        // Service rate represents vehicles/second that can be processed
        double random = Math.random();
        return (random < rate) ? 1 : 0;
    }
    
    // Fixed adaptive green time calculation
    private int calculateAdaptiveGreenTime(TrafficState state, SimulationConfig config) {
        double phase1Density = state.getPhase1Density();
        double phase2Density = state.getPhase2Density();
        double currentDensity, waitingDensity;
        
        if (state.getCurrentPhase() == TrafficPhase.PHASE_1) {
            currentDensity = phase1Density;
            waitingDensity = phase2Density;
        } else {
            currentDensity = phase2Density;
            waitingDensity = phase1Density;
        }
        
        int minGreenTime = config.getMinGreenTime();
        int maxGreenTime = config.getMaxGreenTime();
        
        // If no cars in current direction, use minimum time
        if (currentDensity <= 0.1) {
            return minGreenTime;
        }
        
        // If no cars waiting, extend current green time
        if (waitingDensity <= 0.1) {
            return maxGreenTime;
        }
        
        // Standard adaptive formula: G(t) = T_min + (T_max - T_min) × (D_current / (D_current + D_waiting + ε))
        double ratio = currentDensity / (currentDensity + waitingDensity + 1.0);
        int adaptiveTime = minGreenTime + (int)((maxGreenTime - minGreenTime) * ratio);
        
        return Math.max(minGreenTime, Math.min(maxGreenTime, adaptiveTime));
    }
    
    // Fixed phase switching logic
    private boolean shouldSwitchPhase(TrafficState state, SimulationConfig config) {
        double phase1Density = state.getPhase1Density();
        double phase2Density = state.getPhase2Density();
        
        int currentGreenTime = state.getCurrentGreenTime();
        int minGreenTime = config.getMinGreenTime();
        int maxGreenTime = config.getMaxGreenTime();
        double switchingThreshold = config.getSwitchingThreshold();
        
        // Ensure minimum green time is respected (safety requirement)
        if (currentGreenTime < minGreenTime) {
            return false;
        }
        
        // Force switch if maximum green time is reached
        if (currentGreenTime >= maxGreenTime) {
            log.debug("Switching: Max green time reached ({}s)", currentGreenTime);
            return true;
        }
        
        // Adaptive switching based on traffic demand
        if (state.getCurrentPhase() == TrafficPhase.PHASE_1) {
            // Currently N-S green, check if E-W needs priority
            boolean shouldSwitch = phase2Density > switchingThreshold * phase1Density;
            if (shouldSwitch) {
                log.debug("Switching P1->P2: E-W density ({}) > {} × N-S density ({})", 
                        phase2Density, switchingThreshold, phase1Density);
            }
            return shouldSwitch;
        } else {
            // Currently E-W green, check if N-S needs priority
            boolean shouldSwitch = phase1Density > switchingThreshold * phase2Density;
            if (shouldSwitch) {
                log.debug("Switching P2->P1: N-S density ({}) > {} × E-W density ({})", 
                        phase1Density, switchingThreshold, phase2Density);
            }
            return shouldSwitch;
        }
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
        
        // Create initial state with some vehicles to match scenario
        int initialNorth = 0, initialSouth = 0, initialEast = 0, initialWest = 0;
        
        // Adjust initial state based on scenario
        if ("HEAVY_NS".equals(request.getScenario().toString())) {
            initialNorth = 3;
            initialSouth = 4;
            initialEast = 1;
            initialWest = 1;
        } else if ("BALANCED".equals(request.getScenario().toString())) {
            initialNorth = 2;
            initialSouth = 2;
            initialEast = 2;
            initialWest = 2;
        } else if ("RUSH_HOUR".equals(request.getScenario().toString())) {
            initialNorth = 5;
            initialSouth = 6;
            initialEast = 4;
            initialWest = 5;
        }
        
        TrafficState initialState = TrafficState.builder()
                .simulationId(simulationId)
                .timeStep(0L)
                .timestamp(LocalDateTime.now())
                .vehiclesNorth(initialNorth)
                .vehiclesSouth(initialSouth)
                .vehiclesEast(initialEast)
                .vehiclesWest(initialWest)
                .pedestriansNorth(1)
                .pedestriansSouth(1)
                .pedestriansEast(0)
                .pedestriansWest(0)
                .currentPhase(TrafficPhase.PHASE_1)
                .currentGreenTime(0)
                .calculatedGreenTime(config.getMinGreenTime())
                .phase1Density((double)(initialNorth + initialSouth))
                .phase2Density((double)(initialEast + initialWest))
                .build();
        
        trafficStateRepository.save(initialState);
        
        log.info("Created simulation: {} with scenario: {}, initial vehicles: N={}, S={}, E={}, W={}", 
                simulationId, request.getScenario(), initialNorth, initialSouth, initialEast, initialWest);
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
        private ScheduledFuture<?> scheduledTask;
        private TrafficState currentState; // Fixed: maintain state reference
        
        public SimulationRunner(String simulationId, SimulationConfig config, SimulationRequest request) {
            this.simulationId = simulationId;
            this.config = config;
            this.request = request;
        }
        
        public void start() {
            if (running) return;
            
            running = true;
            status = "RUNNING";
            
            // Get initial state
            currentState = trafficStateRepository.findLatestBySimulationId(simulationId)
                    .orElseThrow(() -> new IllegalArgumentException("No initial state found"));
            
            currentTimeStep = currentState.getTimeStep();
            
            long totalSteps = request.getDurationSeconds();
            int stepInterval = request.getTimeStepMillis();
            
            // Fixed: proper state management in loop
            scheduledTask = executorService.scheduleAtFixedRate(() -> {
                try {
                    if (!running || currentTimeStep >= totalSteps) {
                        stop();
                        return;
                    }
                    
                    // Calculate next state
                    TrafficState nextState = calculateNextState(currentState, config);
                    trafficStateRepository.save(nextState);
                    
                    // Update current state reference (CRITICAL FIX)
                    currentState = nextState;
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
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            
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
                    .currentState(currentState != null ? convertToResponse(currentState) : null)
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