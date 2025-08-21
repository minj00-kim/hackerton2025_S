package com.hackerton.hackerton2025.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import com.hackerton.hackerton2025.Entity.DealType;
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_owner",      columnList = "owner_id"),
                @Index(name = "idx_posts_category",   columnList = "category"),
                @Index(name = "idx_posts_created",    columnList = "created_at"),

                // 지역 탐색용 인덱스 (단일 + 복합)
                @Index(name = "idx_posts_sido_code",  columnList = "sido_code"),
                @Index(name = "idx_posts_sgg_code",   columnList = "sgg_code"),
                @Index(name = "idx_posts_dong_code",  columnList = "dong_code"),
                @Index(name = "idx_posts_region_codes", columnList = "sido_code, sgg_code, dong_code"),

                // 지도 바운딩박스 검색 최적화
                @Index(name = "idx_posts_lat_lng",    columnList = "latitude, longitude")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 255)
    private String address;

    private Double latitude;
    private Double longitude;

    @Column(length = 50)
    private String category;

    // 지역(법정동) 정보
    @Column(name = "sido",    length = 20) private String sido;      // 예) 서울특별시
    @Column(name = "sigungu", length = 30) private String sigungu;   // 예) 강서구
    @Column(name = "dong",    length = 50) private String dong;      // 예) 공항동

    // 법정동 코드
    @Column(name = "sido_code", length = 2)   private String sidoCode; // 예) 11
    @Column(name = "sgg_code",  length = 5)   private String sggCode;  // 예) 11500
    @Column(name = "dong_code", length = 10)  private String dongCode; // 예) 11500560

    // 이미지 URL들
    @ElementCollection
    @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "image_url", length = 512)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    // 로그인 대신 쿠키 anon_id
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

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
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.AVAILABLE;

    // 서비스 레벨에서 한 번에 세팅할 때 유틸
    public void setRegion(String sido, String sigungu, String dong,
                          String sidoCode, String sggCode, String dongCode) {
        this.sido = sido;
        this.sigungu = sigungu;
        this.dong = dong;
        this.sidoCode = sidoCode;
        this.sggCode = sggCode;
        this.dongCode = dongCode;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_type", length = 20, nullable = false)        // ⬅ 추가
    @Builder.Default
    private DealType dealType = DealType.SALE;                         // 기본값: 매매

    @Column(name = "price")                                            // ⬅ 추가
    private Long price;        // SALE: 매매가, JEONSE: 전세보증금

    @Column(name = "deposit")                                          // ⬅ 추가
    private Long deposit;      // MONTHLY 전용 보증금(없으면 0)

    @Column(name = "rent_monthly")                                     // ⬅ 추가
    private Long rentMonthly;  // MONTHLY 전용 월세(원)

    @Column(name = "maintenance_fee")                                  // ⬅ 추가
    private Long maintenanceFee; // 관리비(원/월)

    @Column(name = "area_m2")
    private Double areaM2;

// 매물 조회
    @Column(name = "fav_count", nullable = false)
    @Builder.Default
    private long favCount = 0L;

    @Column(name = "views", nullable = false)
    @Builder.Default
    private long views = 0L;
}
