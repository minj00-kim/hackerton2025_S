// src/main/java/com/hackerton/hackerton2025/Entity/Review.java
package com.hackerton.hackerton2025.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        indexes = { @Index(name = "idx_review_post", columnList = "post_id") },                 // ✅ 변경
        uniqueConstraints = { @UniqueConstraint(columnNames = {"post_id", "user_id"}) }         // ✅ 변경
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 없이 세션/쿠키에서 발급한 익명 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ✅ Listing → Post 기준으로 변경
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)  // ✅ 컬럼명도 post_id 로
    private Post post;

    @Column(nullable = false)
    private Integer rating; // 1~5

    @Column(length = 30, nullable = false)
    private String comment;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
