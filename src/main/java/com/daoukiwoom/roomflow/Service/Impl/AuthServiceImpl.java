package com.daoukiwoom.roomflow.Service.Impl;

import com.daoukiwoom.roomflow.DTO.Request.LoginRequest;
import com.daoukiwoom.roomflow.DTO.Response.AuthResponse;
import com.daoukiwoom.roomflow.DTO.Response.UserResponse;
import com.daoukiwoom.roomflow.Entity.User;
import com.daoukiwoom.roomflow.Exception.AppException;
import com.daoukiwoom.roomflow.Exception.ErrorCode;
import com.daoukiwoom.roomflow.Mapper.UserMapper;
import com.daoukiwoom.roomflow.Repository.IUserRepository;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Security.TokenProvider;
import com.daoukiwoom.roomflow.Service.Interface.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserMapper userMapper;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.isActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        return new AuthResponse(tokenProvider.issue(user), userMapper.toUserResponse(user));
    }

    @Override
    public UserResponse me(AuthUser authUser) {
        return userMapper.toUserResponse(userRepository.findById(authUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }
}
