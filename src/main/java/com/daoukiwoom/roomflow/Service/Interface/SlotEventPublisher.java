package com.daoukiwoom.roomflow.Service.Interface;

import java.time.Instant;
import java.util.UUID;

public interface SlotEventPublisher {
    void publish(String type, UUID roomId, Instant startAt, Instant endAt);
}
