package com.traffic.service;

import com.traffic.dto.TrafficStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    public void sendTrafficUpdate(String simulationId, TrafficStateResponse state) {
        try {
            String destination = "/topic/simulation/" + simulationId;
            messagingTemplate.convertAndSend(destination, state);
            log.debug("Sent traffic update for simulation: {}", simulationId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message for simulation: {}", simulationId, e);
        }
    }
    
    public void sendSimulationStatus(String simulationId, String status, String message) {
        try {
            String destination = "/topic/simulation/" + simulationId + "/status";
            messagingTemplate.convertAndSend(destination, new StatusMessage(status, message));
            log.debug("Sent status update for simulation: {} - {}", simulationId, status);
        } catch (Exception e) {
            log.error("Failed to send status message for simulation: {}", simulationId, e);
        }
    }
    
    public void broadcastSystemMessage(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/system", new SystemMessage(message));
            log.debug("Broadcast system message: {}", message);
        } catch (Exception e) {
            log.error("Failed to broadcast system message", e);
        }
    }
    
    // Helper classes for WebSocket messages
    public static class StatusMessage {
        public String status;
        public String message;
        public long timestamp;
        
        public StatusMessage(String status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class SystemMessage {
        public String message;
        public long timestamp;
        
        public SystemMessage(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}