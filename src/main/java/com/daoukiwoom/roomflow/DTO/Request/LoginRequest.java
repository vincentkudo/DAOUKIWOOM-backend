package com.daoukiwoom.roomflow.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "VALIDATION_FAILED")
    @Email(message = "VALIDATION_FAILED")
    private String email;

    @NotBlank(message = "VALIDATION_FAILED")
    private String password;
}
