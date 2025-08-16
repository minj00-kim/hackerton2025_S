package com.hackerton.hackerton2025.Entity;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString



public class User {

    String name;
    String email;
    String password;
    Boolean state;

    public enum Role {
        USER,//사용자
        ADMIN//중개자 혹은
    }


}
