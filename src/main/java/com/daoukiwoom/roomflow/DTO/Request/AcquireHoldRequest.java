package com.daoukiwoom.roomflow.DTO.Request;

import jakarta.validation.constraints.NotNull;
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
public class AcquireHoldRequest {
    @NotNull
    private UUID roomId;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;
}
