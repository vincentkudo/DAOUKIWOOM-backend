package com.daoukiwoom.roomflow.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateBookingRequest {
    @NotNull
    private UUID roomId;

    @NotBlank
    @Size(max = 160)
    private String title;

    @Size(max = 400)
    private String purpose;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    @Min(1)
    private int attendeeCount;

    private Set<UUID> attendeeIds;

    private UUID holdId;
}
