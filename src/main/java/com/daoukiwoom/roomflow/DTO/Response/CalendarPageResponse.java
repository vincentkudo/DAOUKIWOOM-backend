package com.daoukiwoom.roomflow.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendarPageResponse {
    private PageResponse<RoomResponse> rooms;
    private List<BookingResponse> bookings;
}
