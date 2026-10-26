package com.daoukiwoom.roomflow.DTO.Request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequest {
    @Size(max = 400)
    private String reason;
}
