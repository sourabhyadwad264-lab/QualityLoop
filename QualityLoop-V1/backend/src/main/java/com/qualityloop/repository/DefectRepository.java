package com.qualityloop.repository;

import com.qualityloop.entity.Defect;
import com.qualityloop.entity.DefectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DefectRepository extends JpaRepository<Defect, Long> {
    List<Defect> findByStatusOrderByCreatedAtDesc(DefectStatus status);
    List<Defect> findAllByOrderByCreatedAtDesc();
}
