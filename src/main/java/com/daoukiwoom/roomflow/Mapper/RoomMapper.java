package com.daoukiwoom.roomflow.Mapper;

import com.daoukiwoom.roomflow.DTO.Request.RoomRequest;
import com.daoukiwoom.roomflow.DTO.Response.RoomResponse;
import com.daoukiwoom.roomflow.Entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(target = "locked", ignore = true)
    RoomResponse toRoomResponse(Room room);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Room toRoom(RoomRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateRoom(RoomRequest request, @MappingTarget Room room);
}
