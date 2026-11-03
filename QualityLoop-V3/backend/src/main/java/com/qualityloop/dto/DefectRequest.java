package com.qualityloop.dto;

import com.qualityloop.entity.DefectSource;
import com.qualityloop.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DefectRequest(
        @NotBlank(message = "Title is required") @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotBlank(message = "Part number is required") @Size(max = 60) String partNumber,
        @Size(max = 100) String partName,
        DefectSource source,
        @Positive(message = "Quantity must be 1 or more") Integer quantityAffected,
        LocalDate detectedOn,
        @Size(max = 100) String detectedBy,
        @NotNull(message = "Severity is required") Severity severity,
        @Size(max = 1000) String containmentAction,
        @Size(max = 1000) String rootCause,
        @Size(max = 1000) String remarks) {
}
