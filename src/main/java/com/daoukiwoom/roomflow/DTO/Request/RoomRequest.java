package com.daoukiwoom.roomflow.DTO.Request;

import com.daoukiwoom.roomflow.Entity.Amenity;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {
    @NotBlank
    @Size(max = 16)
    private String code;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Min(1)
    @Max(50)
    private int floor;

    @NotBlank
    @Size(max = 160)
    private String location;

    @Min(1)
    @Max(200)
    private int capacity;

    @Size(max = 500)
    private String description;

    @NotNull
    private RoomStatus status;

    private Set<Amenity> amenities;
}
