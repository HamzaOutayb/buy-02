package com.userservice.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AvatarUpdateRequest {
    @NotBlank(message = "Avatar URL cannot be blank")
    @Size(max = 2048, message = "Avatar URL cannot exceed 2048 characters")
    private String avatar;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}