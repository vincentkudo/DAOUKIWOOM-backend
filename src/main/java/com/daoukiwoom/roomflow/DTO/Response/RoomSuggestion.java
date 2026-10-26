package com.daoukiwoom.roomflow.DTO.Response;

import com.daoukiwoom.roomflow.Entity.FitLevel;
import com.daoukiwoom.roomflow.Entity.SlotState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSuggestion {
    private RoomResponse room;
    private boolean available;
    private SlotState slotState;
    private FitLevel fitLevel;
    private int unusedSeats;
    private String fitLabel;
    private Instant holdExpiresAt;
    private int holdRemainingSeconds;
}
