package com.example.userservice.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RequestUser {
    @NotNull(message = "Email cannot be null")
    @Size(min = 2, message = "Email not be less than two characters")
    @Email
    private String email;

    @NotNull(message = "name cannot be null")
    @Size(min=2, message = "name not be less than two characters")
    private String name;

    @NotNull(message = "Password cannot be null")
    @Size(min=8, message = "Password not be less than two characters")
    private String pwd;
}

