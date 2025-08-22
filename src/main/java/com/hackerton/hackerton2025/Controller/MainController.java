package com.hackerton.hackerton2025.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

public class MainController {
    @GetMapping(value = "/search")
    public String search(Model model) {
        return "user/api/search";
    }


}
