package com.qualityloop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required") @Size(min = 3, max = 40, message = "Username must be 3 to 40 characters") String username,
        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,60}$",
                 message = "Password needs 8 or more characters with an upper case letter, a lower case letter and a number") String password,
        @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
        @Email(message = "Email is not valid") String email) {
}
