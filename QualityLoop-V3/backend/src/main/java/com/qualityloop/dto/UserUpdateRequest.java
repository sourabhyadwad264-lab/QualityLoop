package com.qualityloop.dto;

import com.qualityloop.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRequest(
        @NotNull(message = "Role is required") Role role,
        @NotNull(message = "Active flag is required") Boolean active,
        Long managerId) {
}
