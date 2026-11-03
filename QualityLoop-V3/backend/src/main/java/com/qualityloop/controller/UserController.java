package com.qualityloop.controller;

import com.qualityloop.dto.UserResponse;
import com.qualityloop.dto.UserUpdateRequest;
import com.qualityloop.entity.LoginEvent;
import com.qualityloop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Admin only: checked twice, once in SecurityConfig (URL rule) and once here (@PreAuthorize)
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping
    public List<UserResponse> list() { return service.list(); }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest r, Authentication auth) {
        return service.update(id, r, auth.getName());
    }

    @PatchMapping("/{id}/unlock")
    public UserResponse unlock(@PathVariable Long id) { return service.unlock(id); }

    @GetMapping("/login-events")
    public List<LoginEvent> loginEvents() { return service.recentLoginEvents(); }
}
