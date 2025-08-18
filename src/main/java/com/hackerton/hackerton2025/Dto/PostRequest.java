package com.hackerton.hackerton2025.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    private String category;

    // 업로드된 이미지의 공개 URL 목록
    private List<String> imageUrls;
}
