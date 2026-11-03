package com.qualityloop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Defect {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Severity severity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DefectStatus status = DefectStatus.OPEN;

	private String reportedBy;

	@Column(length = 1000)
	private String containmentAction;

	@Column(length = 1000)
	private String rootCause;

	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime closedAt;

	// One defect has many corrective actions. CorrectiveAction owns the foreign
	// key.
	   @OneToMany(mappedBy = "defect", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private List<CorrectiveAction> actions = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Severity getSeverity() {
		return severity;
	}

	public void setSeverity(Severity severity) {
		this.severity = severity;
	}

	public DefectStatus getStatus() {
		return status;
	}

	public void setStatus(DefectStatus status) {
		this.status = status;
	}

	public String getReportedBy() {
		return reportedBy;
	}

	public void setReportedBy(String reportedBy) {
		this.reportedBy = reportedBy;
	}

	public String getContainmentAction() {
		return containmentAction;
	}

	public void setContainmentAction(String containmentAction) {
		this.containmentAction = containmentAction;
	}

	public String getRootCause() {
		return rootCause;
	}

	public void setRootCause(String rootCause) {
		this.rootCause = rootCause;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(LocalDateTime closedAt) {
		this.closedAt = closedAt;
	}

	public List<CorrectiveAction> getActions() {
		return actions;
	}
}
