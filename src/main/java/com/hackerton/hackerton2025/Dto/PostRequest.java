package com.hackerton.hackerton2025.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotNull(message = "유저 ID는 필수입니다.")
    private Long userId;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    private Double latitude;
    private Double longitude;

    private String category;
}
