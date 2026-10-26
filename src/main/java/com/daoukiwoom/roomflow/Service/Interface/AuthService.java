package com.daoukiwoom.roomflow.Service.Interface;

import com.daoukiwoom.roomflow.DTO.Request.LoginRequest;
import com.daoukiwoom.roomflow.DTO.Response.AuthResponse;
import com.daoukiwoom.roomflow.DTO.Response.UserResponse;
import com.daoukiwoom.roomflow.Security.AuthUser;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    UserResponse me(AuthUser authUser);
}
