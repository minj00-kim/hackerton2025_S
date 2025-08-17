// src/main/java/com/hackerton/hackerton2025/Entity/Favorite.java
package com.hackerton.hackerton2025.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}),
        indexes = {
                @Index(name = "idx_fav_post", columnList = "post_id"),
                @Index(name = "idx_fav_user", columnList = "user_id"),
                @Index(name = "idx_fav_created", columnList = "created_at")
        }
)
public class Favorite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId; // anon_id

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="post_id", nullable=false)   // ✅ Listing → Post
    private Post post;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
