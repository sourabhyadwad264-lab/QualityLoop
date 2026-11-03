package com.qualityloop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// One row for every login attempt, successful or not. Gives the Admin a security trail.
@Entity
@Table(name = "login_event")
public class LoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private boolean success;
    private String ipAddress;
    private String reason;
    private LocalDateTime at = LocalDateTime.now();

    public LoginEvent() {}
    public LoginEvent(String username, boolean success, String ipAddress, String reason) {
        this.username = username;
        this.success = success;
        this.ipAddress = ipAddress;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public boolean isSuccess() { return success; }
    public String getIpAddress() { return ipAddress; }
    public String getReason() { return reason; }
    public LocalDateTime getAt() { return at; }
}
