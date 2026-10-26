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
public class HoldResponse {
    private UUID id;
    private UUID roomId;
    private Instant startAt;
    private Instant endAt;
    private Instant expiresAt;
    private int ttlSeconds;
    private int remainingSeconds;
}
