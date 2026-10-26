package com.daoukiwoom.roomflow.Service.Impl;

import com.daoukiwoom.roomflow.DTO.Response.UserResponse;
import com.daoukiwoom.roomflow.Mapper.UserMapper;
import com.daoukiwoom.roomflow.Repository.IUserRepository;
import com.daoukiwoom.roomflow.Service.Interface.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<UserResponse> listActive() {
        return userRepository.findAllByActiveTrueOrderByFullNameAsc()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }
}
