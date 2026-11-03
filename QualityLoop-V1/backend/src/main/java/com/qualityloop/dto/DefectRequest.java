package com.qualityloop.dto;

import com.qualityloop.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DefectRequest(
        @NotBlank(message = "Title is required") @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull(message = "Severity is required") Severity severity,
        String reportedBy,
        @Size(max = 1000) String containmentAction,
        @Size(max = 1000) String rootCause) {
}
