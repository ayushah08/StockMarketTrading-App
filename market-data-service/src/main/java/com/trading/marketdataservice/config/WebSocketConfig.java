package com.trading.marketdataservice.config;

/*

 1 client connects to ws endpoint
 2 client subs to the /topic/prices/symbol
 3server publishes prices updates to that topic
 4 client receives updates in real time
 */

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Topics prefuixid with / topic -> BROADCASST TO ALL THE SUBS
        registry.enableSimpleBroker("/topic");

        //App Dest Prefix
        //Client Messages to app are routes to @messagemapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //websocket endpoint - clients connect here
        //SockJS fallback for browsers that dont support ws

        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
