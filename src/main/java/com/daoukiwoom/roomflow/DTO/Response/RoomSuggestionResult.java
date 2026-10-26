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
public class RoomSuggestionResult {
    private int attendeeCount;
    private int recommendedCount;
    private List<RoomSuggestion> recommended;
    private List<RoomSuggestion> held;
    private List<RoomSuggestion> occupied;
    private int page;
    private int totalPages;
    private boolean hasMore;
    private boolean amenityFallback;
}
