// src/main/java/com/hackerton/hackerton2025/Entity/Favorite.java
package com.hackerton.hackerton2025.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "favorite", // ✅ 테이블명 고정 (혼동 방지)
        uniqueConstraints = @UniqueConstraint(
                name = "uq_fav_user_post",
                columnNames = {"user_id", "post_id"}
        ),
        indexes = {
                @Index(name = "idx_fav_post", columnList = "post_id"),
                @Index(name = "idx_fav_user", columnList = "user_id"),
                @Index(name = "idx_fav_created", columnList = "created_at"),
                @Index(name = "idx_fav_user_created", columnList = "user_id, created_at") // ✅ 내 찜 최신순 최적화
        }
)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId; // anon_id

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="post_id", nullable=false,
            foreignKey = @ForeignKey(name = "fk_fav_post"))
    @OnDelete(action = OnDeleteAction.CASCADE) // ✅ 매물 삭제 시 함께 삭제
    private Post post;

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable = false)
    private LocalDateTime createdAt;
}
