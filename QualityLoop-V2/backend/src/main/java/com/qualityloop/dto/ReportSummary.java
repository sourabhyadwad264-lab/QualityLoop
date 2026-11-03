package com.qualityloop.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReportSummary(long total, long open, long inProgress, long closed,
                            Map<String, Long> bySeverity, List<PartCount> topParts,
                            List<OverdueAction> overdue) {

    public record PartCount(String partNumber, long count) {
    }

    public record OverdueAction(Long defectId, String defectTitle, String action, String owner, LocalDate dueDate) {
    }
}
