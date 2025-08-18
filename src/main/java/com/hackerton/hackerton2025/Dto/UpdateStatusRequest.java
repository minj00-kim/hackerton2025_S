package com.hackerton.hackerton2025.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    @NotBlank
    private String status; // AVAILABLE | RESERVED | SOLD
}