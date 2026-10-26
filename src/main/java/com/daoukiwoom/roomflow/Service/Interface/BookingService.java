package com.daoukiwoom.roomflow.Service.Interface;

import com.daoukiwoom.roomflow.DTO.Request.CancelBookingRequest;
import com.daoukiwoom.roomflow.DTO.Request.CreateBookingRequest;
import com.daoukiwoom.roomflow.DTO.Response.BookingResponse;
import com.daoukiwoom.roomflow.DTO.Response.CalendarPageResponse;
import com.daoukiwoom.roomflow.DTO.Response.PageResponse;
import com.daoukiwoom.roomflow.Security.AuthUser;

import java.time.Instant;
import java.util.UUID;

public interface BookingService {
    BookingResponse create(CreateBookingRequest request, AuthUser actor);

    BookingResponse update(UUID id, CreateBookingRequest request, AuthUser actor);

    BookingResponse cancel(UUID id, CancelBookingRequest request, AuthUser actor);

    BookingResponse get(UUID id, AuthUser actor);

    PageResponse<BookingResponse> listMine(AuthUser actor, Integer page, Integer size);

    PageResponse<BookingResponse> listRange(Instant from, Instant to, Integer page, Integer size);

    CalendarPageResponse calendar(Instant from, Instant to, Integer offset, Integer size);
}
