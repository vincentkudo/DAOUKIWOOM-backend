package com.daoukiwoom.roomflow.Repository;

import com.daoukiwoom.roomflow.DTO.Response.HoldRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class HoldStore {

    private static final String HOLD = "roomflow:hold:";
    private static final String ROOM = "roomflow:holds:room:";
    private static final String USER = "roomflow:hold:user:";
    private static final String LOCK = "roomflow:hold:lock:";

    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public HoldStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public boolean tryLockRoom(UUID roomId, String token) {
        Boolean locked = redis.opsForValue().setIfAbsent(LOCK + roomId, token, Duration.ofSeconds(5));
        return Boolean.TRUE.equals(locked);
    }

    public void unlockRoom(UUID roomId, String token) {
        redis.execute(RELEASE_LOCK, List.of(LOCK + roomId), token);
    }

    public void save(HoldRecord hold, Duration ttl) {
        redis.opsForValue().set(HOLD + hold.getId(), write(hold), ttl);
        redis.opsForSet().add(ROOM + hold.getRoomId(), hold.getId().toString());
        redis.opsForValue().set(USER + hold.getUserId(), hold.getId().toString(), ttl);
    }

    public Optional<HoldRecord> find(UUID holdId) {
        return read(redis.opsForValue().get(HOLD + holdId));
    }

    public Optional<HoldRecord> findByUser(UUID userId) {
        String holdId = redis.opsForValue().get(USER + userId);
        if (holdId == null) {
            return Optional.empty();
        }
        Optional<HoldRecord> hold = find(UUID.fromString(holdId));
        if (hold.isEmpty()) {
            redis.delete(USER + userId);
        }
        return hold;
    }

    public List<HoldRecord> listActiveByRoom(UUID roomId, Instant now) {
        Set<String> ids = redis.opsForSet().members(ROOM + roomId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<HoldRecord> active = new ArrayList<>();
        for (String id : ids) {
            Optional<HoldRecord> hold = find(UUID.fromString(id));
            if (hold.isEmpty() || hold.get().isExpired(now)) {
                redis.opsForSet().remove(ROOM + roomId, id);
                continue;
            }
            active.add(hold.get());
        }
        return active;
    }

    public void delete(HoldRecord hold) {
        redis.delete(HOLD + hold.getId());
        redis.opsForSet().remove(ROOM + hold.getRoomId(), hold.getId().toString());
        String current = redis.opsForValue().get(USER + hold.getUserId());
        if (hold.getId().toString().equals(current)) {
            redis.delete(USER + hold.getUserId());
        }
    }

    private String write(HoldRecord hold) {
        try {
            return objectMapper.writeValueAsString(hold);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize hold", e);
        }
    }

    private Optional<HoldRecord> read(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, HoldRecord.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
