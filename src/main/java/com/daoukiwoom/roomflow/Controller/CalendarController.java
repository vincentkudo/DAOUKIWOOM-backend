package com.daoukiwoom.roomflow.Controller;

import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import com.daoukiwoom.roomflow.DTO.Response.BookingResponse;
import com.daoukiwoom.roomflow.Service.Interface.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public APIResponse<List<BookingResponse>> week(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        APIResponse<List<BookingResponse>> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.listRange(from, to));
        return apiResponse;
    }
}
