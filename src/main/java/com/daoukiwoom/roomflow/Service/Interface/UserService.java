package com.daoukiwoom.roomflow.Service.Interface;

import com.daoukiwoom.roomflow.DTO.Response.PageResponse;
import com.daoukiwoom.roomflow.DTO.Response.UserResponse;

public interface UserService {
    PageResponse<UserResponse> listActive(Integer page, Integer size);
}
