package fr.agroscan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationWebSocketBroadcaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationWebSocketBroadcaster.class);

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByEmail = new ConcurrentHashMap<>();

    public void register(String email, WebSocketSession session) {
        sessionsByEmail.computeIfAbsent(email.toLowerCase(), ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String email, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByEmail.get(email.toLowerCase());
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) sessionsByEmail.remove(email.toLowerCase());
    }

    public void send(String email, Object payload) {
        Set<WebSocketSession> sessions = sessionsByEmail.get(email.toLowerCase());
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage message = toMessage(payload);
        if (message == null) return;
        sessions.removeIf(session -> !session.isOpen() || !send(session, message));
    }

    private boolean send(WebSocketSession session, TextMessage message) {
        try {
            session.sendMessage(message);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private TextMessage toMessage(Object payload) {
        try {
            return new TextMessage(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Notification WebSocket payload is invalid", exception);
            return null;
        }
    }
}
