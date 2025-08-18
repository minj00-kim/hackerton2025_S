package com.hackerton.hackerton2025.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir; // application.properties에 설정한 경로(기본 uploads)

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 단일 파일 저장 → 공개 URL(/uploads/...) 반환 */
    public String saveOne(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "빈 파일입니다.");
        }
        validateContentType(file);

        try {
            // 날짜별 폴더
            String datePath = LocalDate.now().format(FMT);           // e.g. 2025/08/18
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dir = base.resolve(datePath);
            Files.createDirectories(dir);

            // 파일명: UUID_원본파일명(공백/특수문자 정리)
            String safeOriginal = sanitize(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "_" + safeOriginal;

            Path target = dir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // 브라우저에서 접근 가능한 URL 경로 반환 (WebConfig에서 /uploads/** 매핑했음)
            String url = "/uploads/" + datePath + "/" + filename;
            // 윈도우 경로 구분자 보정
            return url.replace("\\", "/");
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패");
        }
    }

    /** 여러 파일 저장 → URL 목록 반환 */
    public List<String> saveAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        List<String> urls = new ArrayList<>(files.size());
        for (MultipartFile f : files) {
            urls.add(saveOne(f));
        }
        return urls;
    }

    private void validateContentType(MultipartFile file) {
        String ct = Optional.ofNullable(file.getContentType()).orElse("");
        if (!ALLOWED_TYPES.contains(ct)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식: " + ct);
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        // 경로 분리자 제거 + 공백/특수문자 정리
        String n = name.replaceAll("[\\\\/]+", "_")
                .replaceAll("[^\\p{L}\\p{N}._-]+", "_")
                .replaceAll("_+", "_");
        // 파일명 길이 과도할 때 잘라내기
        if (n.length() > 120) n = n.substring(n.length() - 120);
        return n;
    }
}
