package com.hackerton.hackerton2025.Config;

import com.hackerton.hackerton2025.Entity.Listing;
import com.hackerton.hackerton2025.Repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
@RequiredArgsConstructor
public class DevDataInit {
    private final ListingRepository listingRepository;

    @Bean
    CommandLineRunner init() {
        return args -> {
            if (listingRepository.count() == 0) {
                listingRepository.save(Listing.builder().title("테스트 매물").build());
            }
        };
    }
}
