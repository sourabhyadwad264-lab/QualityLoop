package com.qualityloop.dto;

import com.qualityloop.entity.Role;

public record AuthResponse(String token, String username, String fullName, Role role) {
}
