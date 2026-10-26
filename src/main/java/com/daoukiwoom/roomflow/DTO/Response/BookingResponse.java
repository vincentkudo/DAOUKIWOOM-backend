package com.daoukiwoom.roomflow.DTO.Response;

import com.daoukiwoom.roomflow.Entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private RoomResponse room;
    private UserResponse organizer;
    private String title;
    private String purpose;
    private Instant startAt;
    private Instant endAt;
    private int attendeeCount;
    private BookingStatus status;
    private String cancelReason;
    private Instant cancelledAt;
    private Set<UUID> attendeeIds;
    private Instant createdAt;
}
