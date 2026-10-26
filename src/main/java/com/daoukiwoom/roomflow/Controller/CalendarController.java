package com.daoukiwoom.roomflow.Controller;

import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import com.daoukiwoom.roomflow.DTO.Response.CalendarPageResponse;
import com.daoukiwoom.roomflow.Service.Interface.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public APIResponse<CalendarPageResponse> week(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        int limit = size == null || size < 1 ? 20 : size;
        int start = offset != null ? offset : (page == null ? 0 : page * limit);
        APIResponse<CalendarPageResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(bookingService.calendar(from, to, start, limit));
        return apiResponse;
    }
}
