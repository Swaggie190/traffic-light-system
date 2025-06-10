package com.traffic.model;

public enum TrafficPhase {
    PHASE_1(1, "N-S Green, E-W Red"),
    PHASE_2(2, "E-W Green, N-S Red");
    
    private final int value;
    private final String description;
    
    TrafficPhase(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public int getValue() { return value; }
    public String getDescription() { return description; }
}



