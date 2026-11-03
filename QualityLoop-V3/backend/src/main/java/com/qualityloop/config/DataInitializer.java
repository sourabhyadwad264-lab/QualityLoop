package com.qualityloop.config;

import com.qualityloop.entity.AppUser;
import com.qualityloop.entity.Role;
import com.qualityloop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates the first Admin account when the user table is empty.
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminPassword;

    public DataInitializer(UserRepository users, PasswordEncoder encoder,
                           @Value("${app.admin.password}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setFullName("Administrator");
            admin.setRole(Role.ADMIN);
            admin.setPassword(encoder.encode(adminPassword));
            users.save(admin);
            System.out.println(">>> First Admin created. Username: admin");
        }
    }
}
