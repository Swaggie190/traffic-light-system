package com.traffic.model;

import com.traffic.model.TrafficScenario;

public enum TrafficScenario {
    BALANCED("Balanced Traffic"),
    HEAVY_NS("Heavy N-S Traffic"),
    RUSH_HOUR("Rush Hour");
    
    private final String description;
    
    TrafficScenario(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}
