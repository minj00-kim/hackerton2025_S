package com.hackerton.hackerton2025.Controller;

import com.hackerton.hackerton2025.Support.CategoryRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meta")
public class MetaController {

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(CategoryRegistry.CATEGORIES);
    }


}
