package com.daoukiwoom.roomflow.Service.Impl;

import com.daoukiwoom.roomflow.DTO.Response.PageResponse;
import com.daoukiwoom.roomflow.DTO.Response.UserResponse;
import com.daoukiwoom.roomflow.Mapper.UserMapper;
import com.daoukiwoom.roomflow.Repository.IUserRepository;
import com.daoukiwoom.roomflow.Service.Interface.UserService;
import com.daoukiwoom.roomflow.Service.PageLimits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> listActive(Integer page, Integer size) {
        return PageResponse.of(
                userRepository.findAllByActiveTrueOrderByFullNameAsc(PageLimits.of(page, size, PageLimits.USERS))
                        .map(userMapper::toUserResponse)
        );
    }
}
