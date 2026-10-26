package com.daoukiwoom.roomflow.Service.Interface;

import com.daoukiwoom.roomflow.DTO.Request.CancelBookingRequest;
import com.daoukiwoom.roomflow.DTO.Request.CreateBookingRequest;
import com.daoukiwoom.roomflow.DTO.Response.BookingResponse;
import com.daoukiwoom.roomflow.Security.AuthUser;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BookingService {
    BookingResponse create(CreateBookingRequest request, AuthUser actor);

    BookingResponse update(UUID id, CreateBookingRequest request, AuthUser actor);

    BookingResponse cancel(UUID id, CancelBookingRequest request, AuthUser actor);

    BookingResponse get(UUID id, AuthUser actor);

    List<BookingResponse> listMine(AuthUser actor);

    List<BookingResponse> listRange(Instant from, Instant to);
}
