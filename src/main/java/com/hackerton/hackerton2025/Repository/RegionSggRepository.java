package com.hackerton.hackerton2025.Repository;

import com.hackerton.hackerton2025.Entity.RegionSgg;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RegionSggRepository extends JpaRepository<RegionSgg, String> {
    List<RegionSgg> findBySidoCode(String sidoCode);
}
