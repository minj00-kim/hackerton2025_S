package com.hackerton.hackerton2025.Controller;


import com.hackerton.hackerton2025.Dto.TopDataDto;
import com.hackerton.hackerton2025.Service.TopDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topdata")
@RequiredArgsConstructor


public class TopDataController {


    private final TopDataService svc;

    // 예: GET /api/topdata?start=2019-01-01&end=2023-10-31
    @GetMapping
    public TopDataDto getTopData(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) throws Exception {
        return svc.run(start, end);
    }


}
