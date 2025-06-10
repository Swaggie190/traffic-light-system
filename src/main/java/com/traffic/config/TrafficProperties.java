package com.traffic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "traffic")
public class TrafficProperties {
    private SimulationConfig simulation;
    private ParametersConfig parameters;

    // Getters and Setters
    public SimulationConfig getSimulation() { return simulation; }
    public void setSimulation(SimulationConfig simulation) { this.simulation = simulation; }
    public ParametersConfig getParameters() { return parameters; }
    public void setParameters(ParametersConfig parameters) { this.parameters = parameters; }

    public static class SimulationConfig {
        private int timeStep;
        private String defaultScenario;

        public int getTimeStep() { return timeStep; }
        public void setTimeStep(int timeStep) { this.timeStep = timeStep; }
        public String getDefaultScenario() { return defaultScenario; }
        public void setDefaultScenario(String defaultScenario) { this.defaultScenario = defaultScenario; }
    }

    public static class ParametersConfig {
        private int minGreenTime;
        private int maxGreenTime;
        private int yellowTime;
        private int redClearanceTime;
        private double pedestrianWeight;
        private double switchingThreshold;
        private double vehiclePerformanceWeight;
        private double pedestrianPerformanceWeight;

        // Getters and Setters
        public int getMinGreenTime() { return minGreenTime; }
        public void setMinGreenTime(int minGreenTime) { this.minGreenTime = minGreenTime; }
        public int getMaxGreenTime() { return maxGreenTime; }
        public void setMaxGreenTime(int maxGreenTime) { this.maxGreenTime = maxGreenTime; }
        public int getYellowTime() { return yellowTime; }
        public void setYellowTime(int yellowTime) { this.yellowTime = yellowTime; }
        public int getRedClearanceTime() { return redClearanceTime; }
        public void setRedClearanceTime(int redClearanceTime) { this.redClearanceTime = redClearanceTime; }
        public double getPedestrianWeight() { return pedestrianWeight; }
        public void setPedestrianWeight(double pedestrianWeight) { this.pedestrianWeight = pedestrianWeight; }
        public double getSwitchingThreshold() { return switchingThreshold; }
        public void setSwitchingThreshold(double switchingThreshold) { this.switchingThreshold = switchingThreshold; }
        public double getVehiclePerformanceWeight() { return vehiclePerformanceWeight; }
        public void setVehiclePerformanceWeight(double vehiclePerformanceWeight) { this.vehiclePerformanceWeight = vehiclePerformanceWeight; }
        public double getPedestrianPerformanceWeight() { return pedestrianPerformanceWeight; }
        public void setPedestrianPerformanceWeight(double pedestrianPerformanceWeight) { this.pedestrianPerformanceWeight = pedestrianPerformanceWeight; }
    }
}