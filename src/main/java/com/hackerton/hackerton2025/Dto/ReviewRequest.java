package com.hackerton.hackerton2025.Dto;

import jakarta.validation.constraints.*;
public record ReviewRequest
        ( @NotNull @Min(1) @Max(5) Integer rating, @NotBlank @Size(max = 30) String comment ) {}