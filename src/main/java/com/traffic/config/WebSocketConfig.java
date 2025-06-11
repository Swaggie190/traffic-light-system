package com.traffic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                    "http://localhost:48752", // Your current Vite frontend port
                    "http://127.0.0.1:48752",
                    "http://localhost:4000",  // Your intended Vite frontend
                    "http://127.0.0.1:4000",
                    "http://localhost:3000",  // Backup for other React dev servers
                    "http://localhost:5173"   // Backup for other Vite dev servers
                )
                .withSockJS();
    }
}