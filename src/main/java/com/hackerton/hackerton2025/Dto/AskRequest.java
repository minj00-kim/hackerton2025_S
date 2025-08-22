package com.hackerton.hackerton2025.Dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class AskRequest {

    @NotBlank
    private String question;
    private String system; // 선택: 시스템 프롬프트
}
