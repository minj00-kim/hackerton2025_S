package com.hackerton.hackerton2025.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostResponse {

    private Long id;          // Post의 PK
    private String title;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String category;
    private Long ownerId;     // 작성자(User)의 PK

}
