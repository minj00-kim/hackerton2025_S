// src/main/java/com/hackerton/hackerton2025/Entity/Favorite.java
package com.hackerton.hackerton2025.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","listing_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId; // anon_id

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="listing_id", nullable=false)
    private Listing listing;

    @Column(name="created_at", nullable=false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
