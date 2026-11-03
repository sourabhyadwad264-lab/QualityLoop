package com.qualityloop.dto;

import com.qualityloop.entity.AppUser;
import com.qualityloop.entity.Role;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String fullName, String email, Role role,
                           boolean active, Long managerId, String managerName, boolean locked) {

    public static UserResponse from(AppUser u) {
        AppUser m = u.getManager();
        boolean locked = u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now());
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(),
                u.isActive(), m == null ? null : m.getId(), m == null ? null : m.getFullName(), locked);
    }
}
