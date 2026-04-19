package com.sai.finance.finance_manager.marketdata.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.finance.finance_manager.marketdata.service.MarketDataSubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataWebSocketHandler implements WebSocketHandler {

    private final MarketDataWebSocketBroadcaster broadcaster;
    private final WebSocketSessionSubscriptionManager sessionSubscriptionManager;
    private final MarketDataSubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.addSession(session);
        log.info("Client connected: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            String payload = message.getPayload().toString();
            JsonNode root = objectMapper.readTree(payload);

            String action = root.path("action").asText(null);
            if (action == null) return;

            action = action.toLowerCase();

            // Collect NSE symbols
            Set<String> symbols = new HashSet<>();

            if (root.hasNonNull("symbol")) {
                symbols.add(root.get("symbol").asText().toUpperCase().trim());
            }

            if (root.has("symbols") && root.get("symbols").isArray()) {
                for (JsonNode node : root.get("symbols")) {
                    if (node.isTextual()) {
                        symbols.add(node.asText().toUpperCase().trim());
                    }
                }
            }

            if (symbols.isEmpty()) return;

            if (action.equals("subscribe")) {
                for (String sym : symbols) {
                    sessionSubscriptionManager.subscribe(session.getId(), sym);
                    subscriptionManager.subscribe(sym);
                }
                log.info("Session {} subscribed to {}", session.getId(), symbols);

            } else if (action.equals("unsubscribe")) {
                for (String sym : symbols) {
                    sessionSubscriptionManager.unsubscribe(session.getId(), sym);
                    subscriptionManager.unsubscribe(sym);
                }
                log.info("Session {} unsubscribed from {}", session.getId(), symbols);
            }

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.removeSession(session);
        sessionSubscriptionManager.removeSession(session.getId());
        log.info("Client disconnected: {}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void broadcastPrice(com.sai.finance.finance_manager.marketdata.dto.PriceDto dto) {
        broadcaster.broadcastPrice(dto);
    }
}
