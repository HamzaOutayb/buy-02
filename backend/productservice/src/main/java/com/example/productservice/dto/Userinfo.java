package com.example.productservice.dto;

public class Userinfo {
    private String userId;
    private String role;

    public Userinfo(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}