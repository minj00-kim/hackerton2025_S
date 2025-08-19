package com.hackerton.hackerton2025.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// src/main/java/.../Service/AroundService.java
@Service
@RequiredArgsConstructor
public class AroundService {
    private final KakaoPlacesService kakao; // 아래에 있는 서비스

    // 내부 카테고리 목록
    private static final List<String> INTERNAL = List.of(
            "카페/디저트","식당","주점/호프","편의","패션/액세서리","뷰티/미용",
            "의료/약국","문화/취미","레저/스포츠","사무/공유오피스","숙박",
            "창고/물류","팝업/쇼룸","기타"
    );

    public Map<String,Integer> summary(double lat, double lng, int radiusM) {

        // 1) 그룹코드로 바로 집계
        int cafe = kakao.countByGroup(lat, lng, "CE7", radiusM);
        int foodAll = kakao.countByGroup(lat, lng, "FD6", radiusM);
        int conv = kakao.countByGroup(lat, lng, "CS2", radiusM) + kakao.countByGroup(lat, lng, "MT1", radiusM);
        int lodging = kakao.countByGroup(lat, lng, "AD5", radiusM);
        int medical = kakao.countByGroup(lat, lng, "HP8", radiusM) + kakao.countByGroup(lat, lng, "PM9", radiusM);
        int culture = kakao.countByGroup(lat, lng, "CT1", radiusM);

        // 2) FD6 내부에서 '주점/호프'와 '디저트' 골라내기 (페이지 3장=최대 45건 스캔)
        var fd6Docs = kakao.fetchCategoryDocs(lat, lng, "FD6", radiusM);
        int pub = (int) fd6Docs.stream().filter(p ->
                p.categoryName().matches(".*(주점|호프|술집|포차|BAR|Pub|이자카야|와인바|칵테일).*")
        ).count();
        int dessertInFD6 = (int) fd6Docs.stream().filter(p ->
                p.categoryName().matches(".*(빵집|제과|제빵|베이커리|디저트|도넛|케이크|아이스크림|빙수).*")
        ).count();

        int cafeDessert = cafe + dessertInFD6;
        int restaurant = Math.max(0, foodAll - pub - dessertInFD6);

        // 3) 키워드 기반 카테고리
        int fashion = kakao.countByKeywords(lat,lng,radiusM, List.of("의류","옷가게","패션","잡화","신발","가방","액세서리","모자"));
        int beauty  = kakao.countByKeywords(lat,lng,radiusM, List.of("미용실","헤어","이발","네일","왁싱","피부","에스테틱","마사지"));
        int hobby   = culture + kakao.countByKeywords(lat,lng,radiusM, List.of("서점","만화카페","보드게임","PC방","노래방","VR"));
        int sports  = kakao.countByKeywords(lat,lng,radiusM, List.of("헬스장","체육관","필라테스","요가","클라이밍","수영장","볼링장","탁구장"));
        int office  = kakao.countByKeywords(lat,lng,radiusM, List.of("공유오피스","코워킹","비즈니스센터"));
        int warehouse = kakao.countByKeywords(lat,lng,radiusM, List.of("창고","물류센터","보관"));
        int popup   = kakao.countByKeywords(lat,lng,radiusM, List.of("팝업스토어","쇼룸"));

        // 기타는 일단 0으로
        return new LinkedHashMap<>() {{
            put("카페/디저트", cafeDessert);
            put("식당", restaurant);
            put("주점/호프", pub);
            put("편의", conv);
            put("패션/액세서리", fashion);
            put("뷰티/미용", beauty);
            put("의료/약국", medical);
            put("문화/취미", hobby);
            put("레저/스포츠", sports);
            put("사무/공유오피스", office);
            put("숙박", lodging);
            put("창고/물류", warehouse);
            put("팝업/쇼룸", popup);
            put("기타", 0);
        }};
    }

    public List<KakaoPlacesService.Poi> places(double lat,double lng,int radius,String internal, int size){
        return switch (internal) {
            case "카페/디저트" -> kakao.listByGroupOrKeywords(lat,lng,radius,size,
                    List.of("CE7"), List.of("빵집","베이커리","디저트","도넛","케이크","아이스크림","빙수"));
            case "식당" -> kakao.listFoodFiltered(lat,lng,radius,size,
                    "^(?!.*(주점|호프|술집|포차|BAR|Pub|이자카야|와인바|칵테일|빵집|베이커리|디저트|도넛|케이크|아이스크림|빙수)).*$");
            case "주점/호프" -> kakao.listFoodFiltered(lat,lng,radius,size, "(주점|호프|술집|포차|BAR|Pub|이자카야|와인바|칵테일)");
            case "편의" -> kakao.listByGroup(lat,lng,radius,size, List.of("CS2","MT1"));
            case "의료/약국" -> kakao.listByGroup(lat,lng,radius,size, List.of("HP8","PM9"));
            case "숙박" -> kakao.listByGroup(lat,lng,radius,size, List.of("AD5"));
            case "문화/취미" -> kakao.listByGroupOrKeywords(lat,lng,radius,size, List.of("CT1"),
                    List.of("서점","만화카페","보드게임","PC방","노래방","VR"));
            case "패션/액세서리" -> kakao.listByKeywords(lat,lng,radius,size, List.of("의류","옷가게","패션","잡화","신발","가방","액세서리","모자"));
            case "뷰티/미용" -> kakao.listByKeywords(lat,lng,radius,size, List.of("미용실","헤어","이발","네일","왁싱","피부","에스테틱","마사지"));
            case "레저/스포츠" -> kakao.listByKeywords(lat,lng,radius,size, List.of("헬스장","체육관","필라테스","요가","클라이밍","수영장","볼링장","탁구장"));
            case "사무/공유오피스" -> kakao.listByKeywords(lat,lng,radius,size, List.of("공유오피스","코워킹","비즈니스센터"));
            case "창고/물류" -> kakao.listByKeywords(lat,lng,radius,size, List.of("창고","물류센터","보관"));
            case "팝업/쇼룸" -> kakao.listByKeywords(lat,lng,radius,size, List.of("팝업스토어","쇼룸"));
            default -> List.of();
        };
    }
}
