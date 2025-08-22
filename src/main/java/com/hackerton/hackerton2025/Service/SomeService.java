package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Config.ExportProps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class SomeService {


    private final ExportProps exportProps;

    public void doSomething() {
        String baseDir = exportProps.getCrawlDir();
        // ... baseDir 사용
    }
}
