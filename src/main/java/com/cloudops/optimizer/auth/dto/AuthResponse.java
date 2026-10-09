package com.cloudops.optimizer.auth.dto;

import com.cloudops.optimizer.user.Role;

public class AuthResponse {

    private String token;
    private String email;
    private String fullName;
    private Role role;

    public AuthResponse(String token, String email, String fullName, Role role) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }
}
