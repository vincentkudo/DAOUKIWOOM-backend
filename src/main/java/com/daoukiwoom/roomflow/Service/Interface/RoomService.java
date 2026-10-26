package com.daoukiwoom.roomflow.Service.Interface;

import com.daoukiwoom.roomflow.DTO.Request.RoomRequest;
import com.daoukiwoom.roomflow.DTO.Response.PageResponse;
import com.daoukiwoom.roomflow.DTO.Response.RoomResponse;
import com.daoukiwoom.roomflow.DTO.Response.RoomSuggestionResult;
import com.daoukiwoom.roomflow.DTO.Response.RoomSummaryResponse;
import com.daoukiwoom.roomflow.Entity.Amenity;
import com.daoukiwoom.roomflow.Security.AuthUser;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface RoomService {
    PageResponse<RoomResponse> list(Integer page, Integer size);

    RoomSummaryResponse summary();

    RoomResponse get(UUID id);

    RoomResponse create(RoomRequest request);

    RoomResponse update(UUID id, RoomRequest request);

    void delete(UUID id);

    RoomSuggestionResult suggest(
            int attendeeCount,
            Instant startAt,
            Instant endAt,
            Set<Amenity> amenities,
            AuthUser actor,
            Integer page,
            Integer size
    );
}
