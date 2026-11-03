package com.qualityloop.controller;

import com.qualityloop.dto.AuthResponse;
import com.qualityloop.dto.LoginRequest;
import com.qualityloop.dto.RegisterRequest;
import com.qualityloop.dto.UserResponse;
import com.qualityloop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService service;

    public AuthController(UserService service) { this.service = service; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest r) { return service.register(r); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return service.login(r); }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) { return service.me(auth.getName()); }
}
