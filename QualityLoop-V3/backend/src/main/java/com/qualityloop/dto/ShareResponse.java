package com.qualityloop.dto;

import java.time.LocalDateTime;

public record ShareResponse(String token, String url, LocalDateTime expiresAt) {
}
