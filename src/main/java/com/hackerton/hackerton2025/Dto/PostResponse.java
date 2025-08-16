package com.hackerton.hackerton2025.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostResponse {

    private Long user;
    private String location;
    private String price;

    public PostResponse(Long user, String location) {
        this.user = user;
        this.location = location;
        this.price = "$";
    }


}
