package com.daoukiwoom.roomflow.Controller;

import com.daoukiwoom.roomflow.DTO.Request.RoomRequest;
import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import com.daoukiwoom.roomflow.DTO.Response.PageResponse;
import com.daoukiwoom.roomflow.DTO.Response.RoomResponse;
import com.daoukiwoom.roomflow.DTO.Response.RoomSuggestionResult;
import com.daoukiwoom.roomflow.DTO.Response.RoomSummaryResponse;
import com.daoukiwoom.roomflow.Entity.Amenity;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.Interface.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping
    public APIResponse<PageResponse<RoomResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        APIResponse<PageResponse<RoomResponse>> apiResponse = new APIResponse<>();
        apiResponse.setResult(roomService.list(page, size));
        return apiResponse;
    }

    @GetMapping("/summary")
    public APIResponse<RoomSummaryResponse> summary() {
        APIResponse<RoomSummaryResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(roomService.summary());
        return apiResponse;
    }

    @GetMapping("/suggestions")
    public APIResponse<RoomSuggestionResult> suggestions(
            @RequestParam @Min(1) int attendeeCount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endAt,
            @RequestParam(required = false) Set<Amenity> amenities,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthUser actor
    ) {
        APIResponse<RoomSuggestionResult> apiResponse = new APIResponse<>();
        apiResponse.setResult(roomService.suggest(attendeeCount, startAt, endAt, amenities, actor, page, size));
        return apiResponse;
    }

    @GetMapping("/{id}")
    public APIResponse<RoomResponse> get(@PathVariable UUID id) {
        APIResponse<RoomResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(roomService.get(id));
        return apiResponse;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        APIResponse<RoomResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(roomService.create(request));
        return apiResponse;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<RoomResponse> update(@PathVariable UUID id, @Valid @RequestBody RoomRequest request) {
        APIResponse<RoomResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(roomService.update(id, request));
        return apiResponse;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        roomService.delete(id);
        return new APIResponse<>();
    }
}
