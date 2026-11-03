package com.qualityloop.repository;

import com.qualityloop.entity.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {
    List<AuditEntry> findByDefectIdOrderByAtDesc(Long defectId);
}
