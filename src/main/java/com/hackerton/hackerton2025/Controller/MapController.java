package com.hackerton.hackerton2025.Controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class MapController {

    @GetMapping(value = "/search")

    public String search(Model model) {
        // 컨트롤러에서 뷰로 데이터 전달
        model.addAttribute("keyword", "카페");
        model.addAttribute("latitude", 37.5665);
        model.addAttribute("longitude", 126.9780);

        public String search() {
        return "search";

    }


}
