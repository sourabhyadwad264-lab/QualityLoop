package com.qualityloop.security;

import com.qualityloop.entity.AppUser;
import com.qualityloop.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Runs before every request: reads the token and tells Spring Security who the user is.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.parse(header.substring(7));
                // Role, active flag and password-change time are read from the database on every request,
                // so an Admin's change (or a password change) takes effect immediately
                users.findByUsername(claims.getSubject())
                        .filter(u -> u.isActive())
                        .filter(u -> issuedAfterPasswordChange(u, claims))
                        .ifPresent(u -> {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    u.getUsername(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (JwtException | IllegalArgumentException e) {
                // Bad or expired token: the user is simply treated as not logged in
            }
        }
        chain.doFilter(request, response);
    }

    private boolean issuedAfterPasswordChange(AppUser u, Claims claims) {
        if (u.getPasswordChangedAt() == null) return true;
        Instant cutoff = u.getPasswordChangedAt().truncatedTo(ChronoUnit.SECONDS)
                .atZone(ZoneId.systemDefault()).toInstant();
        return claims.getIssuedAt() != null && !claims.getIssuedAt().toInstant().isBefore(cutoff);
    }
}
