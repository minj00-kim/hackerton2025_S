package com.hackerton.hackerton2025.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = "password") // 비번은 로그에 안 찍히게
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "users",
        indexes = { @Index(name = "uk_users_email", columnList = "email", unique = true) }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")         // DB 컬럼명 고정
    private Long userId;              // 리뷰·보안 로직에서 참조할 PK

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;             // 로그인 ID로 사용

    @Column(nullable = false, length = 100)
    private String password;          // BCrypt 해시 저장 (서비스에서 인코딩)

    @Column(nullable = false)
    @Builder.Default
    private Boolean state = true;     // 활성화 여부

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_USER"; // 권한(필요 시)

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (state == null) state = true;
        if (role == null) role = "ROLE_USER";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}