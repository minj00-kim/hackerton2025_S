package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByListing_IdOrderByCreatedAtDesc(Long listingId);
}