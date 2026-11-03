package com.qualityloop.repository;

import com.qualityloop.entity.Defect;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DefectRepository extends JpaRepository<Defect, Long> {
    List<Defect> findAllByOrderByCreatedAtDesc();
    Optional<Defect> findByShareToken(String shareToken);
}
