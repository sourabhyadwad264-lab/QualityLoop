package com.qualityloop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @NotBlank(message = "Recipient email is required") @Email(message = "Recipient email is not valid") String to,
        @Size(max = 500) String message) {
}
