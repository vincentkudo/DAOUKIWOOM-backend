package com.daoukiwoom.roomflow.Config;

import com.daoukiwoom.roomflow.DTO.Response.SlotEvent;
import com.daoukiwoom.roomflow.Service.Interface.SlotEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SlotHub extends TextWebSocketHandler implements SlotEventPublisher {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public SlotHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
    }

    @Override
    public void publish(String type, UUID roomId, Instant startAt, Instant endAt) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new SlotEvent(type, roomId, startAt, endAt));
        } catch (IOException e) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            synchronized (session) {
                try {
                    session.sendMessage(message);
                } catch (IOException ignored) {
                    sessions.remove(session);
                }
            }
        }
    }
}
