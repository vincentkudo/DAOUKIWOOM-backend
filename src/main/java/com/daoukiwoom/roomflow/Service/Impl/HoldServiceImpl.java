package com.daoukiwoom.roomflow.Service.Impl;

import com.daoukiwoom.roomflow.Config.AppClock;
import com.daoukiwoom.roomflow.Config.HoldProperties;
import com.daoukiwoom.roomflow.DTO.Request.AcquireHoldRequest;
import com.daoukiwoom.roomflow.DTO.Response.HoldRecord;
import com.daoukiwoom.roomflow.DTO.Response.HoldResponse;
import com.daoukiwoom.roomflow.DTO.Response.HoldSettingsResponse;
import com.daoukiwoom.roomflow.Entity.Room;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import com.daoukiwoom.roomflow.Exception.AppException;
import com.daoukiwoom.roomflow.Exception.ErrorCode;
import com.daoukiwoom.roomflow.Repository.HoldStore;
import com.daoukiwoom.roomflow.Repository.IBookingRepository;
import com.daoukiwoom.roomflow.Repository.IRoomRepository;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.BookingRules;
import com.daoukiwoom.roomflow.Service.Interface.HoldService;
import com.daoukiwoom.roomflow.Service.Interface.SlotEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class HoldServiceImpl implements HoldService {

    @Autowired
    private HoldStore holdStore;

    @Autowired
    private HoldProperties holdProperties;

    @Autowired
    private IRoomRepository roomRepository;

    @Autowired
    private IBookingRepository bookingRepository;

    @Autowired
    private AppClock appClock;

    @Autowired
    private SlotEventPublisher slotEvents;

    @Override
    public HoldSettingsResponse settings() {
        return new HoldSettingsResponse(holdProperties.getTtlSeconds(), holdProperties.heartbeatSeconds());
    }

    @Override
    public HoldResponse acquire(AcquireHoldRequest request, AuthUser actor) {
        BookingRules.validate(request.getStartAt(), request.getEndAt(), appClock.now(), appClock.zone());
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROOM_INACTIVE);
        }
        if (bookingRepository.existsConfirmedOverlap(room.getId(), request.getStartAt(), request.getEndAt())) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BOOKED);
        }

        String lockToken = UUID.randomUUID().toString();
        lockRoom(room.getId(), lockToken);
        try {
            Instant now = appClock.now();
            HoldRecord conflicting = findForeignHold(room.getId(), request.getStartAt(), request.getEndAt(), actor.getId(), now);
            if (conflicting != null) {
                throw new AppException(ErrorCode.ROOM_HELD_BY_OTHER,
                        "Phòng đang được người khác giữ chỗ. Còn " + conflicting.remainingSeconds(now) + " giây.");
            }
            holdStore.findByUser(actor.getId()).ifPresent(previous -> {
                holdStore.delete(previous);
                slotEvents.publish("RELEASED", previous.getRoomId(), previous.getStartAt(), previous.getEndAt());
            });

            int ttl = holdProperties.getTtlSeconds();
            HoldRecord hold = new HoldRecord(
                    UUID.randomUUID(),
                    room.getId(),
                    actor.getId(),
                    request.getStartAt(),
                    request.getEndAt(),
                    now.plusSeconds(ttl)
            );
            holdStore.save(hold, Duration.ofSeconds(ttl));
            slotEvents.publish("HELD", hold.getRoomId(), hold.getStartAt(), hold.getEndAt());
            return toResponse(hold, ttl, now);
        } finally {
            holdStore.unlockRoom(room.getId(), lockToken);
        }
    }

    @Override
    public HoldResponse heartbeat(UUID holdId, AuthUser actor) {
        HoldRecord hold = requireOwn(holdId, actor);
        Instant now = appClock.now();
        int ttl = holdProperties.getTtlSeconds();
        HoldRecord refreshed = new HoldRecord(
                hold.getId(),
                hold.getRoomId(),
                hold.getUserId(),
                hold.getStartAt(),
                hold.getEndAt(),
                now.plusSeconds(ttl)
        );
        holdStore.save(refreshed, Duration.ofSeconds(ttl));
        return toResponse(refreshed, ttl, now);
    }

    @Override
    public void release(UUID holdId, AuthUser actor) {
        HoldRecord hold = requireOwn(holdId, actor);
        holdStore.delete(hold);
        slotEvents.publish("RELEASED", hold.getRoomId(), hold.getStartAt(), hold.getEndAt());
    }

    @Override
    public void consumeOwn(UUID userId, UUID roomId, Instant startAt, Instant endAt) {
        holdStore.findByUser(userId)
                .filter(hold -> hold.getRoomId().equals(roomId) && hold.overlaps(startAt, endAt))
                .ifPresent(hold -> {
                    holdStore.delete(hold);
                    slotEvents.publish("RELEASED", hold.getRoomId(), hold.getStartAt(), hold.getEndAt());
                });
    }

    @Override
    public void assertNotHeldByOther(UUID roomId, Instant startAt, Instant endAt, UUID userId) {
        Instant now = appClock.now();
        HoldRecord foreign = findForeignHold(roomId, startAt, endAt, userId, now);
        if (foreign != null) {
            throw new AppException(ErrorCode.ROOM_HELD_BY_OTHER,
                    "Phòng đang được người khác giữ chỗ. Còn " + foreign.remainingSeconds(now) + " giây.");
        }
    }

    @Override
    public Optional<HoldRecord> overlappingHold(UUID roomId, Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            return Optional.empty();
        }
        Instant now = appClock.now();
        return holdStore.listActiveByRoom(roomId, now).stream()
                .filter(hold -> hold.overlaps(startAt, endAt))
                .findFirst();
    }

    private HoldRecord findForeignHold(UUID roomId, Instant startAt, Instant endAt, UUID userId, Instant now) {
        return holdStore.listActiveByRoom(roomId, now).stream()
                .filter(hold -> hold.overlaps(startAt, endAt))
                .filter(hold -> !hold.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private HoldRecord requireOwn(UUID holdId, AuthUser actor) {
        HoldRecord hold = holdStore.find(holdId)
                .filter(item -> !item.isExpired(appClock.now()))
                .orElseThrow(() -> new AppException(ErrorCode.HOLD_NOT_FOUND));
        if (!hold.getUserId().equals(actor.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return hold;
    }

    private void lockRoom(UUID roomId, String token) {
        for (int attempt = 0; attempt < 8; attempt++) {
            if (holdStore.tryLockRoom(roomId, token)) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AppException(ErrorCode.ROOM_HOLD_BUSY);
            }
        }
        throw new AppException(ErrorCode.ROOM_HOLD_BUSY);
    }

    private HoldResponse toResponse(HoldRecord hold, int ttl, Instant now) {
        return new HoldResponse(
                hold.getId(),
                hold.getRoomId(),
                hold.getStartAt(),
                hold.getEndAt(),
                hold.getExpiresAt(),
                ttl,
                hold.remainingSeconds(now)
        );
    }
}
