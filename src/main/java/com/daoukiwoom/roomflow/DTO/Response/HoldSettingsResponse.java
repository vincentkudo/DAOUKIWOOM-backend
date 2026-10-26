package com.daoukiwoom.roomflow.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoldSettingsResponse {
    private int ttlSeconds;
    private int heartbeatSeconds;
}
