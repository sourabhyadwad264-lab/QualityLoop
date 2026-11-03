package com.qualityloop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Small helper: who is logged in right now, and what may they do?
public final class CurrentUser {

    private CurrentUser() {}

    public static String name() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }

    public static boolean canClose() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(g ->
                g.getAuthority().equals("ROLE_ADMIN") || g.getAuthority().equals("ROLE_QUALITY_HEAD"));
    }
}
