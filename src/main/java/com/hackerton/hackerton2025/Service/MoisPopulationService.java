package com.hackerton.hackerton2025.Service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import org.w3c.dom.*;

@Service
@RequiredArgsConstructor
public class MoisPopulationService {

    @Value("${mois.base-url}")
    private String baseUrl;

    @Value("${mois.service-key}")
    private String serviceKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** 행정코드(동코드)가 있으면 우선 사용, 없으면 이름 검색 */
    public Optional<PopulationStats> fetchByAdmCodeOrName(String dongCode, String regionLabel) {
        Optional<PopulationStats> byCode = Optional.empty();
        if (dongCode != null && !dongCode.isBlank()) {
            byCode = fetchInternal(Map.of(
                    "pageNo", "1",
                    "numOfRows", "1",
                    // 실제 파라미터 명칭은 API 명세에 맞춰 조정(예: "sig_cd"/"emd_cd"/"locatadd_cd" 등)
                    "locatadd_cd", dongCode
            ));
        }
        if (byCode.isPresent()) return byCode;

        if (regionLabel != null && !regionLabel.isBlank()) {
            // 이름 검색(시군구+읍면동 문자열을 그대로 넘김)
            return fetchInternal(Map.of(
                    "pageNo", "1",
                    "numOfRows", "1",
                    // 실제 파라미터 명칭은 API 명세에 맞춰 조정(예: "locatadd_nm")
                    "locatadd_nm", regionLabel
            ));
        }
        return Optional.empty();
    }

    private Optional<PopulationStats> fetchInternal(Map<String,String> params){
        try {
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("type", "xml");
            for (var e : params.entrySet()) b.queryParam(e.getKey(), e.getValue());
            URI uri = URI.create(b.build(true).toUriString());

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", MediaType.APPLICATION_XML_VALUE)
                    .GET().build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) return Optional.empty();

            return parseXml(res.body());
        } catch (Exception e){
            return Optional.empty();
        }
    }

    /** XML 파서를 관대하게 만들어, 다양한 컬럼명을 포괄적으로 매핑 */
    private Optional<PopulationStats> parseXml(String xml){
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.getBytes()));
            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() == 0) items = doc.getElementsByTagName("row"); // 일부 API는 row

            if (items.getLength() == 0) return Optional.empty();
            Element it = (Element) items.item(0);

            String stdYmd = text(it, List.of("std_ymd","STDR_YYMM","baseYm","base_ym"));
            String name   = text(it, List.of("locatadd_nm","emd_nm","adm_nm","regionNm","ctpv_nm"));

            long total = num(it, List.of("tot_pop","total","totCnt","tot_poplt","totPop","population"));

            // 연령 컬럼 추정 매핑(여러 명칭을 허용)
            Map<String, Long> age = new LinkedHashMap<>();
            putAge(it, age, "0_9",  List.of("age_0_9","agrde_0_9","age0_9","age_00_09"));
            putAge(it, age, "10_19",List.of("age_10_19","agrde_10_19","age10_19"));
            putAge(it, age, "20_29",List.of("age_20_29","agrde_20_29","age20_29"));
            putAge(it, age, "30_39",List.of("age_30_39","agrde_30_39","age30_39"));
            putAge(it, age, "40_49",List.of("age_40_49","agrde_40_49","age40_49"));
            putAge(it, age, "50_59",List.of("age_50_59","agrde_50_59","age50_59"));
            putAge(it, age, "60_69",List.of("age_60_69","agrde_60_69","age60_69"));
            putAge(it, age, "70_plus",List.of("age_70_over","agrde_70_more","age70_over","age_70_79","age_80_over"));

            long male   = num(it, List.of("male","m","m_cnt","ml"));
            long female = num(it, List.of("female","f","f_cnt","fm"));

            return Optional.of(PopulationStats.builder()
                    .stdYmd(stdYmd==null?"":stdYmd)
                    .regionName(name==null?"":name)
                    .total(Math.max(total, sum(age.values())))
                    .male(male).female(female)
                    .ageBuckets(age)
                    .build());
        } catch (Exception e){
            return Optional.empty();
        }
    }

    private static String text(Element it, List<String> keys){
        for (String k : keys){
            NodeList nl = it.getElementsByTagName(k);
            if (nl.getLength() > 0) {
                String v = nl.item(0).getTextContent();
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        return null;
    }
    private static long num(Element it, List<String> keys){
        String v = text(it, keys);
        if (v == null) return 0;
        v = v.replaceAll("[^0-9.-]", "");
        try { return Math.round(Double.parseDouble(v)); } catch(Exception e){ return 0; }
    }
    private static void putAge(Element it, Map<String,Long> map, String key, List<String> keys){
        map.put(key, num(it, keys));
    }
    private static long sum(Collection<Long> c){ long s=0; for (Long x: c) s+=x==null?0:x; return s; }

    @Getter
    @Builder
    public static class PopulationStats {
        private final String stdYmd;
        private final String regionName;
        private final long total;
        private final long male;
        private final long female;
        private final Map<String, Long> ageBuckets; // keys: 0_9,10_19,...,70_plus
    }
}
