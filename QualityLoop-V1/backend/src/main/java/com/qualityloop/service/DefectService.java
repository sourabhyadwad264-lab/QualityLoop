package com.qualityloop.service;

import com.qualityloop.dto.ActionRequest;
import com.qualityloop.dto.DefectRequest;
import com.qualityloop.entity.*;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.AuditEntryRepository;
import com.qualityloop.repository.DefectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DefectService {

	private final DefectRepository defects;
	private final AuditEntryRepository audits;

	public DefectService(DefectRepository defects, AuditEntryRepository audits) {
		this.defects = defects;
		this.audits = audits;
	}

	@Transactional(readOnly = true)
	public List<Defect> list(DefectStatus status) {
		return status == null ? defects.findAllByOrderByCreatedAtDesc()
				: defects.findByStatusOrderByCreatedAtDesc(status);
	}

	@Transactional(readOnly = true)
	public Defect get(Long id) {
		return defects.findById(id).orElseThrow(() -> new NotFoundException("Defect " + id + " not found"));
	}

	public Defect create(DefectRequest r) {
		Defect d = new Defect();
		apply(d, r);
		Defect saved = defects.save(d);
		audits.save(new AuditEntry(saved.getId(), "DEFECT_CREATED", "Severity " + saved.getSeverity()));
		return saved;
	}

	public Defect update(Long id, DefectRequest r) {
		Defect d = get(id);
		requireNotClosed(d);
		apply(d, r);
		audits.save(new AuditEntry(id, "DEFECT_UPDATED", "Details edited"));
		return d;
	}

	public Defect changeStatus(Long id, DefectStatus target) {
		Defect d = get(id);
		requireNotClosed(d);
		if (target == DefectStatus.CLOSED) {
			checkClosureRules(d);
			d.setClosedAt(LocalDateTime.now());
		}
		String detail = d.getStatus() + " -> " + target;
		d.setStatus(target);
		audits.save(new AuditEntry(id, "STATUS_CHANGED", detail));
		return d;
	}

	public Defect addAction(Long defectId, ActionRequest r) {
		Defect d = get(defectId);
		requireNotClosed(d);
		CorrectiveAction a = new CorrectiveAction();
		a.setDescription(r.description());
		a.setOwner(r.owner());
		a.setDueDate(r.dueDate());
		a.setDefect(d);
		d.getActions().add(a);
		defects.flush();
		audits.save(new AuditEntry(defectId, "ACTION_ADDED", r.description()));
		return d;
	}

	public Defect completeAction(Long defectId, Long actionId) {
		Defect d = get(defectId);
		requireNotClosed(d);
		CorrectiveAction a = d.getActions().stream().filter(x -> x.getId().equals(actionId)).findFirst()
				.orElseThrow(() -> new NotFoundException("Action " + actionId + " not found on defect " + defectId));
		a.setStatus(ActionStatus.COMPLETED);
		audits.save(new AuditEntry(defectId, "ACTION_COMPLETED", a.getDescription()));
		return d;
	}

	@Transactional(readOnly = true)
	public List<AuditEntry> history(Long defectId) {
		get(defectId);
		return audits.findByDefectIdOrderByAtDesc(defectId);
	}

	// ---- business rules ----

	private void checkClosureRules(Defect d) {
		if (blank(d.getContainmentAction()))
			throw new BusinessRuleException("Cannot close: containment action is missing");
		if (blank(d.getRootCause()))
			throw new BusinessRuleException("Cannot close: root cause is missing");
		if (d.getActions().isEmpty())
			throw new BusinessRuleException("Cannot close: add at least one corrective action");
		boolean pending = d.getActions().stream().anyMatch(a -> a.getStatus() != ActionStatus.COMPLETED);
		if (pending)
			throw new BusinessRuleException("Cannot close: all corrective actions must be completed");
	}

	private void requireNotClosed(Defect d) {
		if (d.getStatus() == DefectStatus.CLOSED)
			throw new BusinessRuleException("Defect " + d.getId() + " is closed and cannot be changed");
	}

	private void apply(Defect d, DefectRequest r) {
		d.setTitle(r.title());
		d.setDescription(r.description());
		d.setSeverity(r.severity());
		d.setReportedBy(r.reportedBy());
		d.setContainmentAction(r.containmentAction());
		d.setRootCause(r.rootCause());
	}

	private boolean blank(String s) {
		return s == null || s.isBlank();
	}
}
