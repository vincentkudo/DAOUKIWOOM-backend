package com.daoukiwoom.roomflow.Service.Interface;

import com.daoukiwoom.roomflow.DTO.Response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> listActive();
}
