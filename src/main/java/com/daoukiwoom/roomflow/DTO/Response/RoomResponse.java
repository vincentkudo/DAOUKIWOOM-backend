package com.daoukiwoom.roomflow.DTO.Response;

import com.daoukiwoom.roomflow.Entity.Amenity;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private UUID id;
    private String code;
    private String name;
    private int floor;
    private String location;
    private int capacity;
    private String description;
    private RoomStatus status;
    private Set<Amenity> amenities;
}
