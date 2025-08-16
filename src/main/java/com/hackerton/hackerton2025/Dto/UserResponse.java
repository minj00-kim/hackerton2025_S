package com.hackerton.hackerton2025.Dto;


import com.hackerton.hackerton2025.Entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class UserResponse {

    private long id;
    private String name;
    private String email;
    private Boolean state;
    private User.role role;





}
