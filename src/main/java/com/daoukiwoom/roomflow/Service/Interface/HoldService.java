package com.daoukiwoom.roomflow.Service.Interface;

import com.daoukiwoom.roomflow.DTO.Request.AcquireHoldRequest;
import com.daoukiwoom.roomflow.DTO.Response.HoldRecord;
import com.daoukiwoom.roomflow.DTO.Response.HoldResponse;
import com.daoukiwoom.roomflow.DTO.Response.HoldSettingsResponse;
import com.daoukiwoom.roomflow.Security.AuthUser;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface HoldService {
    HoldSettingsResponse settings();

    HoldResponse acquire(AcquireHoldRequest request, AuthUser actor);

    HoldResponse heartbeat(UUID holdId, AuthUser actor);

    void release(UUID holdId, AuthUser actor);

    void consumeOwn(UUID userId, UUID roomId, Instant startAt, Instant endAt);

    void assertNotHeldByOther(UUID roomId, Instant startAt, Instant endAt, UUID userId);

    Optional<HoldRecord> overlappingHold(UUID roomId, Instant startAt, Instant endAt);
}
