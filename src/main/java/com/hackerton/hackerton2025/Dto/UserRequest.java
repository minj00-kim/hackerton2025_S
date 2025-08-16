package com.hackerton.hackerton2025.Dto;


import com.hackerton.hackerton2025.Entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class UserRequest {

    private String name;
    private String email;
    private String password;
    private User.role role;


}
