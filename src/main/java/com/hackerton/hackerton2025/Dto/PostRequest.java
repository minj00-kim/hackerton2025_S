package com.hackerton.hackerton2025.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @NoArgsConstructor @AllArgsConstructor
public class PostRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    private Double latitude;
    private Double longitude;
    private String category;
}