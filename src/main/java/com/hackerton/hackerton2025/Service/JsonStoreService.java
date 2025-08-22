package com.hackerton.hackerton2025.Service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;



@Service


public class JsonStoreService {

    private final ObjectMapper om = new ObjectMapper();

    public List<Map<String,Object>> readArrayJson(Path file) throws IOException {
        return om.readValue(file.toFile(), new TypeReference<>() {});
    }
    public Map<String,Object> readObjectJson(Path file) throws IOException {
        return om.readValue(file.toFile(), new TypeReference<>() {});
    }

    // 타임스탬프 명의 최신 하위 폴더(예: 20250820_1542)
    public Path findLatestDir(Path baseDir) throws IOException {
        try (var s = Files.list(baseDir)) {
            return s.filter(Files::isDirectory)
                    .max(Comparator.comparing(p -> p.getFileName().toString()))
                    .orElseThrow(() -> new IllegalStateException("No subdir under " + baseDir));
        }
    }

}
