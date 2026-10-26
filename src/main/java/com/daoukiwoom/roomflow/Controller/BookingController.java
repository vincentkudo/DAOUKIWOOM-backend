package com.daoukiwoom.roomflow.Controller;

import com.daoukiwoom.roomflow.DTO.Request.CancelBookingRequest;
import com.daoukiwoom.roomflow.DTO.Request.CreateBookingRequest;
import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import com.daoukiwoom.roomflow.DTO.Response.BookingResponse;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.Interface.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/mine")
    public APIResponse<List<BookingResponse>> mine(@AuthenticationPrincipal AuthUser actor) {
        APIResponse<List<BookingResponse>> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.listMine(actor));
        return apiResponse;
    }

    @GetMapping
    public APIResponse<List<BookingResponse>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        APIResponse<List<BookingResponse>> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.listRange(from, to));
        return apiResponse;
    }

    @GetMapping("/{id}")
    public APIResponse<BookingResponse> get(@PathVariable UUID id, @AuthenticationPrincipal AuthUser actor) {
        APIResponse<BookingResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.get(id, actor));
        return apiResponse;
    }

    @PostMapping
    public APIResponse<BookingResponse> create(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthUser actor
    ) {
        APIResponse<BookingResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.create(request, actor));
        return apiResponse;
    }

    @PutMapping("/{id}")
    public APIResponse<BookingResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthUser actor
    ) {
        APIResponse<BookingResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.update(id, request, actor));
        return apiResponse;
    }

    @PostMapping("/{id}/cancel")
    public APIResponse<BookingResponse> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelBookingRequest request,
            @AuthenticationPrincipal AuthUser actor
    ) {
        APIResponse<BookingResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.cancel(id, request, actor));
        return apiResponse;
    }
}
