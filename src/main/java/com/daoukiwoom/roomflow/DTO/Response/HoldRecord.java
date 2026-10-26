package com.daoukiwoom.roomflow.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoldRecord {
    private UUID id;
    private UUID roomId;
    private UUID userId;
    private Instant startAt;
    private Instant endAt;
    private Instant expiresAt;

    public boolean overlaps(Instant start, Instant end) {
        return startAt.isBefore(end) && start.isBefore(endAt);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public int remainingSeconds(Instant now) {
        return (int) Math.max(0, expiresAt.getEpochSecond() - now.getEpochSecond());
    }
}
