package com.hackerton.hackerton2025.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "region_sgg")
public class RegionSgg {

    @Id
    @Column(name = "sgg_code", length = 5)
    private String sggCode;

    @Column(name = "sido_code", length = 2, nullable = false) // ← nullable 철자 주의
    private String sidoCode;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    // getters / setters
}
