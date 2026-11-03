package com.qualityloop.service;

import com.qualityloop.dto.ReportSummary;
import com.qualityloop.dto.ReportView;
import com.qualityloop.dto.ShareResponse;
import com.qualityloop.entity.*;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.AuditEntryRepository;
import com.qualityloop.repository.DefectRepository;
import com.qualityloop.security.CurrentUser;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportService {

    private final DefectRepository defects;
    private final AuditEntryRepository audits;
    private final JavaMailSender mailSender;
    private final String mailUser;
    private final String frontendUrl;

    public ReportService(DefectRepository defects, AuditEntryRepository audits, JavaMailSender mailSender,
                         @Value("${spring.mail.username:}") String mailUser,
                         @Value("${app.frontend-url}") String frontendUrl) {
        this.defects = defects;
        this.audits = audits;
        this.mailSender = mailSender;
        this.mailUser = mailUser;
        this.frontendUrl = frontendUrl;
    }

    // ---------- dashboard numbers ----------
    @Transactional(readOnly = true)
    public ReportSummary summary() {
        List<Defect> all = defects.findAll();
        long open = all.stream().filter(d -> d.getStatus() == DefectStatus.OPEN).count();
        long inProgress = all.stream().filter(d -> d.getStatus() == DefectStatus.IN_PROGRESS).count();
        long closed = all.stream().filter(d -> d.getStatus() == DefectStatus.CLOSED).count();

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Severity s : Severity.values()) {
            bySeverity.put(s.name(), all.stream().filter(d -> d.getSeverity() == s).count());
        }

        Map<String, Long> perPart = all.stream()
                .filter(d -> d.getPartNumber() != null && !d.getPartNumber().isBlank())
                .collect(Collectors.groupingBy(Defect::getPartNumber, Collectors.counting()));
        List<ReportSummary.PartCount> topParts = perPart.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new ReportSummary.PartCount(e.getKey(), e.getValue()))
                .toList();

        List<ReportSummary.OverdueAction> overdue = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Defect d : all) {
            if (d.getStatus() == DefectStatus.CLOSED) continue;
            for (CorrectiveAction a : d.getActions()) {
                if (a.getStatus() == ActionStatus.PENDING && a.getDueDate() != null && a.getDueDate().isBefore(today)) {
                    overdue.add(new ReportSummary.OverdueAction(d.getId(), d.getTitle(), a.getDescription(),
                            a.getOwner(), a.getDueDate()));
                }
            }
        }
        return new ReportSummary(all.size(), open, inProgress, closed, bySeverity, topParts, overdue);
    }

    // ---------- share link (read-only, valid for 7 days) ----------
    public ShareResponse share(Long defectId) {
        Defect d = defects.findById(defectId)
                .orElseThrow(() -> new NotFoundException("Defect " + defectId + " not found"));
        LocalDateTime now = LocalDateTime.now();
        if (d.getShareToken() == null || d.getShareExpiresAt() == null || d.getShareExpiresAt().isBefore(now)) {
            d.setShareToken(UUID.randomUUID().toString());
            d.setShareExpiresAt(now.plusDays(7));
            audits.save(new AuditEntry(defectId, "REPORT_LINK_CREATED", "Valid for 7 days", CurrentUser.name()));
        }
        return new ShareResponse(d.getShareToken(), frontendUrl + "/#/report/" + d.getShareToken(),
                d.getShareExpiresAt());
    }

    // Public: no login needed, only the secret token
    @Transactional(readOnly = true)
    public ReportView publicReport(String token) {
        Defect d = defects.findByShareToken(token)
                .filter(x -> x.getShareExpiresAt() != null && x.getShareExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new NotFoundException("This report link is invalid or has expired"));
        return view(d);
    }

    // ---------- email ----------
    public void emailReport(Long defectId, String to, String note) {
        if (mailUser == null || mailUser.isBlank())
            throw new BusinessRuleException("Email is not set up. Set MAIL_USERNAME and MAIL_PASSWORD, then restart the backend");
        Defect d = defects.findById(defectId)
                .orElseThrow(() -> new NotFoundException("Defect " + defectId + " not found"));
        ReportView v = view(d);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper h = new MimeMessageHelper(message, false, "UTF-8");
            h.setFrom(mailUser);
            h.setTo(to);
            h.setSubject("Defect report #" + v.id() + " - Part " + v.partNumber());
            h.setText(html(v, note), true);
        } catch (MessagingException e) {
            throw new BusinessRuleException("The email could not be prepared: " + e.getMessage());
        }
        mailSender.send(message);
        audits.save(new AuditEntry(defectId, "REPORT_EMAILED", "Sent to " + to, CurrentUser.name()));
    }

    // ---------- helpers ----------
    private ReportView view(Defect d) {
        List<ReportView.ActionView> actions = d.getActions().stream()
                .map(a -> new ReportView.ActionView(a.getDescription(), a.getOwner(), a.getDueDate(), a.getStatus().name()))
                .toList();
        return new ReportView(d.getId(), d.getTitle(), d.getPartNumber(), d.getPartName(),
                d.getSource() == null ? null : d.getSource().name(), d.getQuantityAffected(), d.getDetectedOn(),
                d.getDetectedBy(), d.getSeverity().name(), d.getStatus().name(), d.getDescription(),
                d.getContainmentAction(), d.getRootCause(), d.getCreatedAt(), d.getClosedAt(), actions);
    }

    private String html(ReportView v, String note) {
        StringBuilder s = new StringBuilder("<div style='font-family:Segoe UI,Arial,sans-serif;max-width:640px;color:#1f2933'>");
        s.append("<h2>Defect report #").append(v.id()).append(": ").append(esc(v.title())).append("</h2>");
        if (note != null && !note.isBlank()) s.append("<p>").append(esc(note)).append("</p>");
        s.append("<table style='border-collapse:collapse'>");
        row(s, "Part number", v.partNumber());
        row(s, "Part name", v.partName());
        row(s, "Source", v.source() == null ? null : v.source().replace('_', ' '));
        row(s, "Quantity affected", v.quantityAffected() == null ? null : v.quantityAffected().toString());
        row(s, "Detected on", v.detectedOn() == null ? null : v.detectedOn().toString());
        row(s, "Detected by", v.detectedBy());
        row(s, "Severity", v.severity());
        row(s, "Status", v.status().replace('_', ' '));
        row(s, "Problem", v.description());
        row(s, "Containment action", v.containmentAction());
        row(s, "Root cause", v.rootCause());
        s.append("</table><h3>Corrective actions</h3>");
        if (v.actions().isEmpty()) {
            s.append("<p>No corrective actions recorded yet.</p>");
        } else {
            s.append("<table style='border-collapse:collapse;width:100%' border='1' cellpadding='6'>")
             .append("<tr><th align='left'>Action</th><th align='left'>Owner</th><th align='left'>Due</th><th align='left'>Status</th></tr>");
            for (ReportView.ActionView a : v.actions()) {
                s.append("<tr><td>").append(esc(a.description())).append("</td><td>").append(esc(a.owner()))
                 .append("</td><td>").append(a.dueDate() == null ? "" : a.dueDate().toString())
                 .append("</td><td>").append(a.status()).append("</td></tr>");
            }
            s.append("</table>");
        }
        s.append("<p style='color:#7b8794;font-size:12px'>Sent from QualityLoop</p></div>");
        return s.toString();
    }

    private void row(StringBuilder s, String label, String value) {
        s.append("<tr><td style='padding:4px 14px 4px 0;color:#52606d;vertical-align:top'>").append(label)
         .append("</td><td style='padding:4px 0'>").append(value == null || value.isBlank() ? "-" : esc(value))
         .append("</td></tr>");
    }

    // Escapes user text so it cannot inject HTML into the email
    private String esc(String text) {
        return HtmlUtils.htmlEscape(text == null ? "" : text);
    }
}
