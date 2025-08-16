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
    private Long user;

    @NotBlank(message = "위치는 필수입니다.")
    private String location;

    @NotBlank(message = "가격은 필수입니다.")
    private String price;

}
