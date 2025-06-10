package com.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDetection {
    private Integer north;
    private Integer south;
    private Integer east;
    private Integer west;
    private Integer total;
    
    public Integer getTotal() {
        if (total == null && north != null && south != null && east != null && west != null) {
            total = north + south + east + west;
        }
        return total;
    }
}