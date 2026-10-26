package com.daoukiwoom.roomflow.Controller;

import com.daoukiwoom.roomflow.DTO.Request.LoginRequest;
import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import com.daoukiwoom.roomflow.DTO.Response.AuthResponse;
import com.daoukiwoom.roomflow.DTO.Response.UserResponse;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.Interface.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public APIResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        APIResponse<AuthResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(authService.login(request));
        return apiResponse;
    }

    @GetMapping("/me")
    public APIResponse<UserResponse> me(@AuthenticationPrincipal AuthUser authUser) {
        APIResponse<UserResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(authService.me(authUser));
        return apiResponse;
    }
}
