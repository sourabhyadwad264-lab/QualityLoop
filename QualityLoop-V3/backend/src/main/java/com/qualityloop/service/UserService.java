package com.qualityloop.service;

import com.qualityloop.dto.*;
import com.qualityloop.entity.AppUser;
import com.qualityloop.entity.LoginEvent;
import com.qualityloop.exception.AuthException;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.LoginEventRepository;
import com.qualityloop.repository.UserRepository;
import com.qualityloop.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final LoginEventRepository loginEvents;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final int maxFailedAttempts;
    private final int lockMinutes;

    public UserService(UserRepository users, LoginEventRepository loginEvents, PasswordEncoder encoder, JwtService jwt,
                       @Value("${app.security.max-failed-attempts}") int maxFailedAttempts,
                       @Value("${app.security.lock-minutes}") int lockMinutes) {
        this.users = users;
        this.loginEvents = loginEvents;
        this.encoder = encoder;
        this.jwt = jwt;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
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

    // noRollbackFor: a failed login throws an exception, but the failed-attempt counter
    // and the login event must still be saved. Without this, Spring would undo them.
    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest r, String ip) {
        String name = r.username().trim();
        AppUser u = users.findByUsername(name).orElse(null);
        if (u == null) {
            logEvent(name, false, ip, "UNKNOWN_USER");
            throw new AuthException("Wrong username or password");
        }

        LocalDateTime now = LocalDateTime.now();
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(now)) {
            logEvent(name, false, ip, "ACCOUNT_LOCKED");
            throw new AuthException("Too many wrong passwords. Try again after "
                    + u.getLockedUntil().format(DateTimeFormatter.ofPattern("HH:mm")) + " or ask the Admin to unlock the account");
        }

        if (!u.isActive() || !encoder.matches(r.password(), u.getPassword())) {
            if (u.isActive()) {
                u.setFailedAttempts(u.getFailedAttempts() + 1);
                if (u.getFailedAttempts() >= maxFailedAttempts) {
                    u.setLockedUntil(now.plusMinutes(lockMinutes));
                    u.setFailedAttempts(0);
                }
            }
            logEvent(name, false, ip, u.isActive() ? "WRONG_PASSWORD" : "ACCOUNT_DISABLED");
            throw new AuthException("Wrong username or password");
        }

        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        logEvent(name, true, ip, "LOGIN_OK");
        return new AuthResponse(jwt.generate(u.getUsername(), u.getRole().name()),
                u.getUsername(), u.getFullName(), u.getRole());
    }

    public void changePassword(String username, ChangePasswordRequest r) {
        AppUser u = users.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
        if (!encoder.matches(r.currentPassword(), u.getPassword()))
            throw new BusinessRuleException("Current password is wrong");
        if (encoder.matches(r.newPassword(), u.getPassword()))
            throw new BusinessRuleException("The new password must be different from the current one");
        u.setPassword(encoder.encode(r.newPassword()));
        // Every token issued before this moment stops working
        u.setPasswordChangedAt(LocalDateTime.now());
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

    public UserResponse unlock(Long id) {
        AppUser u = users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        return UserResponse.from(u);
    }

    @Transactional(readOnly = true)
    public List<LoginEvent> recentLoginEvents() {
        return loginEvents.findTop100ByOrderByAtDesc();
    }

    private void logEvent(String username, boolean success, String ip, String reason) {
        loginEvents.save(new LoginEvent(username, success, ip, reason));
    }
}
