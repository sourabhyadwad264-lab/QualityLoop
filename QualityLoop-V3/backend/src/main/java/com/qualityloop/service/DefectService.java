package com.qualityloop.service;

import com.qualityloop.dto.ActionRequest;
import com.qualityloop.dto.DefectRequest;
import com.qualityloop.entity.*;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.ForbiddenException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.AuditEntryRepository;
import com.qualityloop.repository.DefectRepository;
import com.qualityloop.security.CurrentUser;
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
    public List<Defect> list(DefectStatus status, String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return defects.findAllByOrderByCreatedAtDesc().stream()
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> needle.isEmpty() || contains(d.getTitle(), needle)
                        || contains(d.getPartNumber(), needle) || contains(d.getPartName(), needle))
                .toList();
    }

    @Transactional(readOnly = true)
    public Defect get(Long id) {
        return defects.findById(id).orElseThrow(() -> new NotFoundException("Defect " + id + " not found"));
    }

    public Defect create(DefectRequest r) {
        Defect d = new Defect();
        apply(d, r);
        d.setReportedBy(CurrentUser.name());
        Defect saved = defects.save(d);
        audit(saved.getId(), "DEFECT_CREATED", "Part " + saved.getPartNumber() + ", severity " + saved.getSeverity());
        return saved;
    }

    public Defect update(Long id, DefectRequest r) {
        Defect d = get(id);
        requireNotClosed(d);
        apply(d, r);
        audit(id, "DEFECT_UPDATED", "Details edited");
        return d;
    }

    public Defect changeStatus(Long id, DefectStatus target) {
        Defect d = get(id);
        requireNotClosed(d);
        if (target == DefectStatus.CLOSED) {
            if (!CurrentUser.canClose())
                throw new ForbiddenException("Only a Quality Head or Admin can close a defect");
            checkClosureRules(d);
            d.setClosedAt(LocalDateTime.now());
        }
        String detail = d.getStatus() + " -> " + target;
        d.setStatus(target);
        audit(id, "STATUS_CHANGED", detail);
        return d;
    }

    // Only OPEN defects can be removed. A short trace stays in the audit table.
    public void delete(Long id) {
        Defect d = get(id);
        if (d.getStatus() != DefectStatus.OPEN)
            throw new BusinessRuleException("Only OPEN defects can be deleted. Other records are kept for history");
        defects.delete(d);
        audit(id, "DEFECT_DELETED", d.getTitle());
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
        audit(defectId, "ACTION_ADDED", r.description());
        return d;
    }

    public Defect completeAction(Long defectId, Long actionId) {
        Defect d = get(defectId);
        requireNotClosed(d);
        CorrectiveAction a = findAction(d, actionId);
        a.setStatus(ActionStatus.COMPLETED);
        audit(defectId, "ACTION_COMPLETED", a.getDescription());
        return d;
    }

    public void deleteAction(Long defectId, Long actionId) {
        Defect d = get(defectId);
        requireNotClosed(d);
        CorrectiveAction a = findAction(d, actionId);
        d.getActions().remove(a);
        audit(defectId, "ACTION_DELETED", a.getDescription());
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> history(Long defectId) {
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

    private CorrectiveAction findAction(Defect d, Long actionId) {
        return d.getActions().stream()
                .filter(x -> x.getId().equals(actionId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Action " + actionId + " not found on defect " + d.getId()));
    }

    private void apply(Defect d, DefectRequest r) {
        d.setTitle(r.title());
        d.setDescription(r.description());
        d.setPartNumber(r.partNumber().trim());
        d.setPartName(r.partName());
        d.setSource(r.source());
        d.setQuantityAffected(r.quantityAffected());
        d.setDetectedOn(r.detectedOn());
        d.setDetectedBy(r.detectedBy());
        d.setSeverity(r.severity());
        d.setContainmentAction(r.containmentAction());
        d.setRootCause(r.rootCause());
        d.setRemarks(r.remarks());
    }

    private void audit(Long defectId, String event, String detail) {
        audits.save(new AuditEntry(defectId, event, detail, CurrentUser.name()));
    }

    private boolean contains(String text, String needle) {
        return text != null && text.toLowerCase().contains(needle);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
