package com.hackerton.hackerton2025.Dto;

import com.hackerton.hackerton2025.Entity.DealType;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import com.hackerton.hackerton2025.Entity.DealType;
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    private String category;

    // 업로드된 이미지의 공개 URL 목록
    private List<String> imageUrls;

    @NotNull(message = "거래유형(dealType)은 필수입니다.")           // SALE/JEONSE/MONTHLY
    private DealType dealType;

    @PositiveOrZero(message="가격(price)은 0 이상")                 // SALE/JEONSE 에서 사용
    private Long price;

    @PositiveOrZero(message="보증금(deposit)은 0 이상")             // MONTHLY 에서 사용
    private Long deposit;

    @PositiveOrZero(message="월세(rentMonthly)는 0 이상")           // MONTHLY 에서 사용
    private Long rentMonthly;

    @PositiveOrZero(message="관리비(maintenanceFee)는 0 이상")
    private Long maintenanceFee;

    @Positive(message="면적(areaM2)은 0보다 커야 합니다.")
    private Double areaM2;  // m²

    @Pattern(regexp = "^\\+?\\d[\\d\\- ]{7,20}$", message = "연락처 형식이 올바르지 않습니다.")
    private String contactPhone;           // 예: 010-1234-5678

    private Boolean moveInImmediate;       // true면 즉시 입주
    private LocalDate moveInDate;          // false일 때 yyyy-MM-dd
}
