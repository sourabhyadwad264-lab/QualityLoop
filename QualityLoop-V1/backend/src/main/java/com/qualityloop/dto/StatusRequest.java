package com.qualityloop.dto;

import com.qualityloop.entity.DefectStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "Status is required") DefectStatus status) {
}
