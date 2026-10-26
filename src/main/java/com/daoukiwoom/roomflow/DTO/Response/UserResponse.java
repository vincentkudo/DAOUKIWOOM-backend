package com.daoukiwoom.roomflow.DTO.Response;

import com.daoukiwoom.roomflow.Entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String department;
    private Role role;
    private boolean active;
}
