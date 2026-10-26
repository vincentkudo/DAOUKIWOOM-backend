package com.daoukiwoom.roomflow.Mapper;

import com.daoukiwoom.roomflow.DTO.Response.UserResponse;
import com.daoukiwoom.roomflow.Entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}
