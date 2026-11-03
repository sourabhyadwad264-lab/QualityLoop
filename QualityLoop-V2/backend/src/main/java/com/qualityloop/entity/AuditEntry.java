package com.qualityloop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long defectId;
    private String event;

    @Column(length = 500)
    private String detail;

    // Who did it
    private String performedBy;

    private LocalDateTime at = LocalDateTime.now();

    public AuditEntry() {}
    public AuditEntry(Long defectId, String event, String detail, String performedBy) {
        this.defectId = defectId;
        this.event = event;
        this.detail = detail;
        this.performedBy = performedBy;
    }

    public Long getId() { return id; }
    public Long getDefectId() { return defectId; }
    public String getEvent() { return event; }
    public String getDetail() { return detail; }
    public String getPerformedBy() { return performedBy; }
    public LocalDateTime getAt() { return at; }
}
