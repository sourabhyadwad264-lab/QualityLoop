package com.qualityloop.service;

import com.qualityloop.dto.*;
import com.qualityloop.entity.AppUser;
import com.qualityloop.exception.AuthException;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.UserRepository;
import com.qualityloop.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public UserService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // New accounts always start as Quality Engineer. Only an Admin can change the role.
    public UserResponse register(RegisterRequest r) {
        String username = r.username().trim();
        if (users.existsByUsername(username))
            throw new BusinessRuleException("Username '" + username + "' is already taken");
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setFullName(r.fullName().trim());
        u.setEmail(r.email());
        u.setPassword(encoder.encode(r.password()));
        return UserResponse.from(users.save(u));
    }

    public AuthResponse login(LoginRequest r) {
        AppUser u = users.findByUsername(r.username().trim())
                .orElseThrow(() -> new AuthException("Wrong username or password"));
        if (!u.isActive() || !encoder.matches(r.password(), u.getPassword()))
            throw new AuthException("Wrong username or password");
        return new AuthResponse(jwt.generate(u.getUsername(), u.getRole().name()),
                u.getUsername(), u.getFullName(), u.getRole());
    }

    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        return UserResponse.from(users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found")));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse update(Long id, UserUpdateRequest r, String actor) {
        AppUser u = users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
        if (u.getUsername().equals(actor))
            throw new BusinessRuleException("You cannot change your own role or status");
        AppUser manager = null;
        if (r.managerId() != null) {
            if (r.managerId().equals(id))
                throw new BusinessRuleException("A user cannot be their own manager");
            manager = users.findById(r.managerId())
                    .orElseThrow(() -> new NotFoundException("Manager " + r.managerId() + " not found"));
        }
        u.setRole(r.role());
        u.setActive(r.active());
        u.setManager(manager);
        return UserResponse.from(u);
    }
}
