package com.qualityloop.repository;

import com.qualityloop.entity.Defect;
import com.qualityloop.entity.DefectStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DefectRepository extends JpaRepository<Defect, Long> {

    List<Defect> findAllByOrderByCreatedAtDesc();
    List<Defect> findByStatus(DefectStatus status);
    Optional<Defect> findByShareToken(String shareToken);

    // JPQL aggregate queries: the database does the counting (GROUP BY), not Java
    @Query("select d.status, count(d) from Defect d group by d.status")
    List<Object[]> countByStatus();

    @Query("select d.severity, count(d) from Defect d group by d.severity")
    List<Object[]> countBySeverity();

    @Query("select d.partNumber, count(d) from Defect d where d.partNumber is not null "
         + "group by d.partNumber order by count(d) desc")
    List<Object[]> countByPart(Pageable pageable);

    @Query("select d.createdAt from Defect d where d.createdAt >= :since")
    List<LocalDateTime> findCreatedSince(@Param("since") LocalDateTime since);
}
