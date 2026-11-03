package com.qualityloop.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ActionRequest(
        @NotBlank(message = "Action description is required") String description,
        String owner,
        LocalDate dueDate) {
}
