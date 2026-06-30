package fr.agroscan.config;

import fr.agroscan.service.NotificationWebSocketBroadcaster;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final JwtDecoder jwtDecoder;
    private final NotificationWebSocketBroadcaster broadcaster;
    private final Map<String, String> emailsBySessionId = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(JwtDecoder jwtDecoder, NotificationWebSocketBroadcaster broadcaster) {
        this.jwtDecoder = jwtDecoder;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String email = authenticate(session.getUri());
        emailsBySessionId.put(session.getId(), email);
        broadcaster.register(email, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = emailsBySessionId.remove(session.getId());
        if (email != null) broadcaster.unregister(email, session);
    }

    private String authenticate(URI uri) {
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token manquant");
        }
        try {
            return jwtDecoder.decode(token).getSubject();
        } catch (JwtException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalide", exception);
        }
    }
}
