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

    private LocalDateTime at = LocalDateTime.now();

    public AuditEntry() {}
    public AuditEntry(Long defectId, String event, String detail) {
        this.defectId = defectId;
        this.event = event;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Long getDefectId() { return defectId; }
    public String getEvent() { return event; }
    public String getDetail() { return detail; }
    public LocalDateTime getAt() { return at; }
}
