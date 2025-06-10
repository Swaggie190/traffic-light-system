package com.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LightState {
    private String color; // RED, YELLOW, GREEN
    private Integer duration; // How long this state will last
    private Boolean pedestrianCrossing; // Can pedestrians cross?
}