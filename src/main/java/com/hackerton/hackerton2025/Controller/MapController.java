package com.hackerton.hackerton2025.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class MapController {

    @GetMapping(value = "/search")
    public String search() {
        return "search";

    }


}
