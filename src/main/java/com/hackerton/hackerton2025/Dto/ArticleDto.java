package com.hackerton.hackerton2025.Dto;


import java.time.OffsetDateTime;
import java.util.List;


public record ArticleDto(
        String title,
        String url,
        String source,
        OffsetDateTime publishedAt,
        String summary,
        Double score,
        List<String> tags
) {}


