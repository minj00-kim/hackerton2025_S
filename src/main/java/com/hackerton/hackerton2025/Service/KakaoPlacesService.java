package com.hackerton.hackerton2025.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KakaoPlacesService {

    @Value("${kakao.api.key:}")
    private String kakaoKey;

    private static final ObjectMapper OM = new ObjectMapper();
    private final RestTemplate rt = new RestTemplate();

    /** 외부로 내보낼 POI 타입 */
    public static record Poi(
            String name,
            String categoryName,
            double lat,
            double lng,
            int distance,
            String placeUrl
    ) {}

    /** 내부 파싱용 */
    public static record Doc(
            String placeName,
            String categoryName,
            double lat,
            double lng,
            int distance,
            String url
    ) {}

    /* ==================== 공통 호출 ==================== */
    private JsonNode call(URI uri) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.set("Authorization", "KakaoAK " + kakaoKey);
            h.set("Accept", "application/json");
            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, new HttpEntity<>(h), String.class);
            if (res.getBody() == null) return OM.createObjectNode();
            return OM.readTree(res.getBody());
        } catch (RestClientResponseException e) { // 400/401/403/429 등
            System.out.println("[KAKAO_API_ERROR] " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
            return OM.createObjectNode();
        } catch (Exception e) {
            System.out.println("[KAKAO_API_ERROR] " + e.getMessage());
            return OM.createObjectNode();
        }
    }

    /* ==================== 카운트 ==================== */
    /** 반경 내 카테고리 그룹 개수 (FD6/CE7/...) */
    @Cacheable(
            cacheNames = "kakao:groupCount",
            key = "#groupCode + ':' + T(java.lang.String).format('%.4f', #lat) + ':' + " +
                    "T(java.lang.String).format('%.4f', #lng) + ':' + #radiusM"
    )
    public int countByGroup(double lat, double lng, String groupCode, int radiusM) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/search/category.json")
                .queryParam("category_group_code", groupCode)
                .queryParam("x", lng).queryParam("y", lat)
                .queryParam("radius", radiusM)
                .queryParam("size", 1)
                .build()
                .toUri();
        return call(uri).path("meta").path("pageable_count").asInt(0);
    }

    /** 키워드들의 pageable_count 합산 (한글 인코딩 필수) */
    @Cacheable(
            cacheNames = "kakao:keywordCount",
            key = "T(java.lang.String).join('|', #keywords) + ':' + " +
                    "T(java.lang.String).format('%.4f', #lat) + ':' + " +
                    "T(java.lang.String).format('%.4f', #lng) + ':' + #radiusM"
    )
    public int countByKeywords(double lat, double lng, int radiusM, List<String> keywords) {
        int sum = 0;
        for (String q : keywords) {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", q)
                    .queryParam("x", lng).queryParam("y", lat)
                    .queryParam("radius", radiusM)
                    .queryParam("size", 1)
                    .build()
                    .encode(StandardCharsets.UTF_8)   // ✅ 인코딩
                    .toUri();
            sum += call(uri).path("meta").path("pageable_count").asInt(0);
        }
        return sum;
    }

    /* ==================== 문서 수집 ==================== */
    /** 카테고리 그룹 문서 최대 45개 수집(3페이지) */
    @Cacheable(
            cacheNames = "kakao:categoryDocs",
            key = "#code + ':' + T(java.lang.String).format('%.4f', #lat) + ':' + " +
                    "T(java.lang.String).format('%.4f', #lng) + ':' + #radius"
    )
    public List<Doc> fetchCategoryDocs(double lat, double lng, String code, int radius) {
        List<Doc> out = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/category.json")
                    .queryParam("category_group_code", code)
                    .queryParam("x", lng).queryParam("y", lat)
                    .queryParam("radius", radius)
                    .queryParam("page", page)
                    .queryParam("size", 15)
                    .build()
                    .toUri();
            JsonNode docs = call(uri).path("documents");
            if (!docs.isArray() || docs.size() == 0) break;
            for (JsonNode d : docs) out.add(toDoc(d));
        }
        return out;
    }

    /** 키워드 문서 최대 45개 수집(3페이지) */
    @Cacheable(
            cacheNames = "kakao:keywordDocs",
            key = "#keyword + ':' + T(java.lang.String).format('%.4f', #lat) + ':' + " +
                    "T(java.lang.String).format('%.4f', #lng) + ':' + #radius"
    )
    public List<Doc> fetchKeywordDocs(double lat, double lng, String keyword, int radius) {
        List<Doc> out = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", keyword)
                    .queryParam("x", lng).queryParam("y", lat)
                    .queryParam("radius", radius)
                    .queryParam("page", page)
                    .queryParam("size", 15)
                    .build()
                    .encode(StandardCharsets.UTF_8)   // ✅ 인코딩
                    .toUri();
            JsonNode docs = call(uri).path("documents");
            if (!docs.isArray() || docs.size() == 0) break;
            for (JsonNode d : docs) out.add(toDoc(d));
        }
        return out;
    }

    /* ==================== 리스트 반환 ==================== */
    public List<Poi> listByGroup(double lat, double lng, int radius, int size, List<String> codes) {
        List<Doc> docs = new ArrayList<>();
        for (String c : codes) docs.addAll(fetchCategoryDocs(lat, lng, c, radius));
        return toPoiUnique(docs, size);
    }

    public List<Poi> listByKeywords(double lat, double lng, int radius, int size, List<String> keywords) {
        List<Doc> docs = new ArrayList<>();
        for (String k : keywords) docs.addAll(fetchKeywordDocs(lat, lng, k, radius));
        return toPoiUnique(docs, size);
    }

    public List<Poi> listByGroupOrKeywords(double lat, double lng, int radius, int size,
                                           List<String> codes, List<String> keywords) {
        List<Doc> docs = new ArrayList<>();
        for (String c : codes) docs.addAll(fetchCategoryDocs(lat, lng, c, radius));
        for (String k : keywords) docs.addAll(fetchKeywordDocs(lat, lng, k, radius));
        return toPoiUnique(docs, size);
    }

    /** 음식점(FD6)에서 정규식 필터로 선별 */
    public List<Poi> listFoodFiltered(double lat, double lng, int radius, int size, String regex) {
        List<Doc> docs = fetchCategoryDocs(lat, lng, "FD6", radius);
        return toPoiUnique(
                docs.stream().filter(d -> d.categoryName().matches(regex)).collect(Collectors.toList()),
                size
        );
    }

    /* ==================== 내부 변환/유틸 ==================== */
    private Doc toDoc(JsonNode d) {
        double lng = d.path("x").asDouble();
        double lat = d.path("y").asDouble();
        return new Doc(
                d.path("place_name").asText(),
                d.path("category_name").asText(""),
                lat, lng,
                d.path("distance").asInt(0),
                d.path("place_url").asText("")
        );
    }

    private Poi toPoi(Doc d) {
        return new Poi(d.placeName(), d.categoryName(), d.lat(), d.lng(), d.distance(), d.url());
    }

    /** place_url 있으면 그걸 키로, 없으면 name+좌표를 키로 해서 중복 제거 */
    private List<Poi> toPoiUnique(List<Doc> docs, int size) {
        Map<String, Doc> uniq = new LinkedHashMap<>();
        for (Doc d : docs) {
            String key = (d.url() != null && !d.url().isBlank())
                    ? d.url()
                    : d.placeName() + "|" + d.lat() + "|" + d.lng();
            uniq.putIfAbsent(key, d);
        }
        return uniq.values().stream().limit(size).map(this::toPoi).toList();
    }
}
