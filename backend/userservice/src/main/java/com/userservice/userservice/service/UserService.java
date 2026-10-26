package com.userservice.userservice.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.dto.UserUpdateRequest;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.exception.ResourceNotFoundException;
import com.userservice.userservice.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(String email) {
        User user = findByEmail(email);
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setRole(user.getRole() == null ? null : user.getRole().name());
        response.setAvatar(user.getAvatar());
        return response;
    }

    public UserResponse updateProfile(String email, UserUpdateRequest request) {
        User user = findByEmail(email);
        user.setName(request.getName().trim());

        if (request.getAvatar() != null) {
            String avatar = request.getAvatar().trim();
            user.setAvatar(avatar.isEmpty() ? null : avatar);
        }
        User savedUser = userRepository.save(user);
        UserResponse response = new UserResponse();
        response.setId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setName(savedUser.getName());
        response.setRole(savedUser.getRole() == null ? null : savedUser.getRole().name());
        response.setAvatar(savedUser.getAvatar());
        return response;
    }

    public UserResponse updateAvatar(String email, String avatarUrl) {
        User user = findByEmail(email);
        user.setAvatar(avatarUrl.trim());
        User savedUser = userRepository.save(user);
        UserResponse response = new UserResponse();
        response.setId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setName(savedUser.getName());
        response.setRole(savedUser.getRole() == null ? null : savedUser.getRole().name());
        response.setAvatar(savedUser.getAvatar());
        return response;
    }
    
    private User findByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
