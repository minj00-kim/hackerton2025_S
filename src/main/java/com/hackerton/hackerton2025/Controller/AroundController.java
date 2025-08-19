package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Service.AroundService;
import com.hackerton.hackerton2025.Service.KakaoGeoService;
import com.hackerton.hackerton2025.Service.KakaoPlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// B. 주소/좌표 기반 검색 전용
@RestController
@RequestMapping("/around")
@RequiredArgsConstructor
public class AroundController {
    private final AroundService around;
    private final KakaoGeoService geo;

    @GetMapping("/summary-by-address")
    public ResponseEntity<Map<String,Object>> summaryByAddress(@RequestParam String address,
                                                               @RequestParam(defaultValue="800") int radius) {
        var g = geo.geocode(address).orElseThrow(() -> new RuntimeException("주소 결과 없음"));
        return ResponseEntity.ok(Map.of(
                "address", address, "lat", g.lat(), "lng", g.lng(), "radius", radius,
                "buckets", around.summary(g.lat(), g.lng(), radius)
        ));
    }

    @GetMapping("/places-by-address")
    public ResponseEntity<List<KakaoPlacesService.Poi>> placesByAddress(@RequestParam String address,
                                                                        @RequestParam String category,
                                                                        @RequestParam(defaultValue="800") int radius,
                                                                        @RequestParam(defaultValue="20") int size) {
        var g = geo.geocode(address).orElseThrow(() -> new RuntimeException("주소 결과 없음"));
        return ResponseEntity.ok(around.places(g.lat(), g.lng(), radius, category, size));
    }
}
