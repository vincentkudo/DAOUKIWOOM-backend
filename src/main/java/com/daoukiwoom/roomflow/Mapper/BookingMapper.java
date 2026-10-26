package com.daoukiwoom.roomflow.Mapper;

import com.daoukiwoom.roomflow.DTO.Response.BookingResponse;
import com.daoukiwoom.roomflow.Entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {RoomMapper.class, UserMapper.class})
public interface BookingMapper {
    BookingResponse toBookingResponse(Booking booking);
}
