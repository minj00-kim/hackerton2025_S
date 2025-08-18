package com.hackerton.hackerton2025.Entity;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "recommends")

public class Recommend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추천 업종 (카페, 치킨, 편의점 등)
    private String category;

    // 추천 점수 (AI/로직 기반)
    private Double score;

    // 추천 근거 설명
    private String reason;

    /** 어떤 게시물(Post)에 대한 추천인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

}
