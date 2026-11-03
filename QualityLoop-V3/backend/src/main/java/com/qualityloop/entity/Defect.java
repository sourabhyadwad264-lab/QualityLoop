package com.qualityloop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "defect", indexes = {
        @Index(name = "idx_defect_part", columnList = "part_number"),
        @Index(name = "idx_defect_status", columnList = "status")
})
public class Defect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "part_number")
    private String partNumber;
    private String partName;

    @Enumerated(EnumType.STRING)
    private DefectSource source;

    private Integer quantityAffected;
    private LocalDate detectedOn;
    private String detectedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DefectStatus status = DefectStatus.OPEN;

    // Set by the server from the logged-in user
    private String reportedBy;

    @Column(length = 1000)
    private String containmentAction;

    @Column(length = 1000)
    private String rootCause;

    @Column(length = 1000)
    private String remarks;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime closedAt;

    // Used for the read-only report link sent to customers
    @JsonIgnore
    private String shareToken;
    @JsonIgnore
    private LocalDateTime shareExpiresAt;

    // EAGER: load the actions together with the defect
    @OneToMany(mappedBy = "defect", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CorrectiveAction> actions = new ArrayList<>();

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public DefectSource getSource() { return source; }
    public void setSource(DefectSource source) { this.source = source; }
    public Integer getQuantityAffected() { return quantityAffected; }
    public void setQuantityAffected(Integer quantityAffected) { this.quantityAffected = quantityAffected; }
    public LocalDate getDetectedOn() { return detectedOn; }
    public void setDetectedOn(LocalDate detectedOn) { this.detectedOn = detectedOn; }
    public String getDetectedBy() { return detectedBy; }
    public void setDetectedBy(String detectedBy) { this.detectedBy = detectedBy; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public DefectStatus getStatus() { return status; }
    public void setStatus(DefectStatus status) { this.status = status; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public String getContainmentAction() { return containmentAction; }
    public void setContainmentAction(String containmentAction) { this.containmentAction = containmentAction; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public LocalDateTime getShareExpiresAt() { return shareExpiresAt; }
    public void setShareExpiresAt(LocalDateTime shareExpiresAt) { this.shareExpiresAt = shareExpiresAt; }
    public List<CorrectiveAction> getActions() { return actions; }
}
