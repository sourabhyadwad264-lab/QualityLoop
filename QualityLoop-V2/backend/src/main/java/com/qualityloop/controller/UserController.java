package com.qualityloop.controller;

import com.qualityloop.dto.UserResponse;
import com.qualityloop.dto.UserUpdateRequest;
import com.qualityloop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Admin only (see SecurityConfig)
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping
    public List<UserResponse> list() { return service.list(); }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest r, Authentication auth) {
        return service.update(id, r, auth.getName());
    }
}
