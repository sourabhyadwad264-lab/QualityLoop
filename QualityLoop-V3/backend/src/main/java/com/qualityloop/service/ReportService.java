package com.qualityloop.service;

import com.qualityloop.dto.ReportSummary;
import com.qualityloop.dto.ReportView;
import com.qualityloop.dto.ShareResponse;
import com.qualityloop.entity.*;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.AuditEntryRepository;
import com.qualityloop.repository.CorrectiveActionRepository;
import com.qualityloop.repository.DefectRepository;
import com.qualityloop.security.CurrentUser;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class ReportService {

    private final DefectRepository defects;
    private final CorrectiveActionRepository actions;
    private final AuditEntryRepository audits;
    private final JavaMailSender mailSender;
    private final String mailUser;
    private final String frontendUrl;

    public ReportService(DefectRepository defects, CorrectiveActionRepository actions, AuditEntryRepository audits,
                         JavaMailSender mailSender,
                         @Value("${spring.mail.username:}") String mailUser,
                         @Value("${app.frontend-url}") String frontendUrl) {
        this.defects = defects;
        this.actions = actions;
        this.audits = audits;
        this.mailSender = mailSender;
        this.mailUser = mailUser;
        this.frontendUrl = frontendUrl;
    }

    // ---------- dashboard numbers (counted by the database with GROUP BY) ----------
    @Transactional(readOnly = true)
    public ReportSummary summary() {
        Map<DefectStatus, Long> byStatus = new EnumMap<>(DefectStatus.class);
        for (Object[] row : defects.countByStatus()) byStatus.put((DefectStatus) row[0], (Long) row[1]);
        long open = byStatus.getOrDefault(DefectStatus.OPEN, 0L);
        long inProgress = byStatus.getOrDefault(DefectStatus.IN_PROGRESS, 0L);
        long closed = byStatus.getOrDefault(DefectStatus.CLOSED, 0L);

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Severity s : Severity.values()) bySeverity.put(s.name(), 0L);
        for (Object[] row : defects.countBySeverity()) bySeverity.put(((Severity) row[0]).name(), (Long) row[1]);

        List<ReportSummary.PartCount> topParts = new ArrayList<>();
        for (Object[] row : defects.countByPart(PageRequest.of(0, 5)))
            topParts.add(new ReportSummary.PartCount((String) row[0], (Long) row[1]));

        List<ReportSummary.OverdueAction> overdue = new ArrayList<>();
        for (CorrectiveAction a : actions.findOverdue(ActionStatus.PENDING, DefectStatus.CLOSED, LocalDate.now()))
            overdue.add(new ReportSummary.OverdueAction(a.getDefect().getId(), a.getDefect().getTitle(),
                    a.getDescription(), a.getOwner(), a.getDueDate()));

        // Average number of days from reporting to closing
        double totalDays = 0;
        int count = 0;
        for (Defect d : defects.findByStatus(DefectStatus.CLOSED)) {
            if (d.getClosedAt() != null) {
                totalDays += ChronoUnit.DAYS.between(d.getCreatedAt().toLocalDate(), d.getClosedAt().toLocalDate());
                count++;
            }
        }
        Double avg = count == 0 ? null : Math.round(totalDays / count * 10.0) / 10.0;

        // Defects reported per month, last 6 months (empty months show 0)
        YearMonth first = YearMonth.now().minusMonths(5);
        List<LocalDateTime> dates = defects.findCreatedSince(first.atDay(1).atStartOfDay());
        List<ReportSummary.MonthCount> byMonth = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            YearMonth ym = first.plusMonths(i);
            long n = dates.stream().filter(t -> YearMonth.from(t).equals(ym)).count();
            byMonth.add(new ReportSummary.MonthCount(ym.toString(), n));
        }

        return new ReportSummary(open + inProgress + closed, open, inProgress, closed,
                bySeverity, topParts, overdue, avg, byMonth);
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
        List<ReportView.ActionView> list = d.getActions().stream()
                .map(a -> new ReportView.ActionView(a.getDescription(), a.getOwner(), a.getDueDate(), a.getStatus().name()))
                .toList();
        return new ReportView(d.getId(), d.getTitle(), d.getPartNumber(), d.getPartName(),
                d.getSource() == null ? null : d.getSource().name(), d.getQuantityAffected(), d.getDetectedOn(),
                d.getDetectedBy(), d.getSeverity().name(), d.getStatus().name(), d.getDescription(),
                d.getContainmentAction(), d.getRootCause(), d.getCreatedAt(), d.getClosedAt(), list);
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
