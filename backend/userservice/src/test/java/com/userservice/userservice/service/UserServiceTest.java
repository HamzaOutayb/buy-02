package com.userservice.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.dto.UserUpdateRequest;
import com.userservice.userservice.entity.Role;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.exception.ResourceNotFoundException;
import com.userservice.userservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getProfileReturnsMappedFieldsAndNormalizesEmail() {
        User user = user("user-1", "alice@example.com", "Alice", Role.CLIENT, "avatar.png");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile(" Alice@Example.com ");

        assertEquals("user-1", response.getId());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("Alice", response.getName());
        assertEquals("CLIENT", response.getRole());
        assertEquals("avatar.png", response.getAvatar());
        verify(userRepository).findByEmail("alice@example.com");
    }

    @Test
    void getProfileReturnsNullRoleWhenUserHasNoRole() {
        User user = user("user-1", "alice@example.com", "Alice", null, null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile("alice@example.com");

        assertNull(response.getRole());
        assertNull(response.getAvatar());
    }

    @Test
    void getProfileThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getProfile(" Missing@Example.com "));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfileTrimsValuesSavesUserAndReturnsUpdatedProfile() {
        User user = user("user-1", "alice@example.com", "Alice", Role.SELLER, "old.png");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);

        UserResponse response = userService.updateProfile(
                " Alice@Example.com ", new UserUpdateRequest("  Alice Updated  ", "  new.png  "));

        verify(userRepository).save(savedUser.capture());
        assertEquals("Alice Updated", savedUser.getValue().getName());
        assertEquals("new.png", savedUser.getValue().getAvatar());
        assertEquals("Alice Updated", response.getName());
        assertEquals("new.png", response.getAvatar());
        assertEquals("SELLER", response.getRole());
    }

    @Test
    void updateProfilePreservesAvatarWhenRequestOmitsIt() {
        User user = user("user-1", "alice@example.com", "Alice", Role.CLIENT, "existing.png");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile(
                "alice@example.com", new UserUpdateRequest("Alice Renamed", null));

        assertEquals("Alice Renamed", response.getName());
        assertEquals("existing.png", response.getAvatar());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileClearsAvatarWhenRequestContainsOnlyWhitespace() {
        User user = user("user-1", "alice@example.com", "Alice", Role.CLIENT, "existing.png");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile(
                "alice@example.com", new UserUpdateRequest("Alice", "   "));

        assertNull(response.getAvatar());
        assertNull(user.getAvatar());
    }

    @Test
    void updateProfileThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateProfile(
                        "missing@example.com", new UserUpdateRequest("Missing", null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAvatarTrimsValueSavesUserAndReturnsUpdatedProfile() {
        User user = user("user-1", "alice@example.com", "Alice", Role.CLIENT, "old.png");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);

        UserResponse response = userService.updateAvatar("alice@example.com", "  new.png  ");

        verify(userRepository).save(savedUser.capture());
        assertEquals("new.png", savedUser.getValue().getAvatar());
        assertEquals("new.png", response.getAvatar());
        assertEquals("alice@example.com", response.getEmail());
    }

    @Test
    void updateAvatarThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateAvatar("missing@example.com", "avatar.png"));

        verify(userRepository, never()).save(any(User.class));
    }

    private User user(String id, String email, String name, Role role, String avatar) {
        return new User(id, email, "encoded-password", name, role, avatar);
    }
}
