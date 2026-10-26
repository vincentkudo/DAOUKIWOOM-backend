package com.daoukiwoom.roomflow.Controller;

import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import com.daoukiwoom.roomflow.DTO.Response.UserResponse;
import com.daoukiwoom.roomflow.Service.Interface.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public APIResponse<List<UserResponse>> list() {
        APIResponse<List<UserResponse>> apiResponse = new APIResponse<>();
        apiResponse.setResult(userService.listActive());
        return apiResponse;
    }
}
