package com.userservice.userservice.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.userservice.userservice.dto.LoginRequest;
import com.userservice.userservice.dto.RegisterRequest;
import com.userservice.userservice.entity.Role;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.exception.DuplicateResourceException;
import com.userservice.userservice.repository.UserRepository;
import com.userservice.userservice.security.JwtService;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // Registers a user with normalized details and an encoded password
    @Test
    void registerSavesNormalizedUser() {
        RegisterRequest request = new RegisterRequest(
                "  Alice@Example.com ", "password123", " Alice Smith ", Role.CLIENT);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        var response = authService.register(request);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("user-1", response.getId());
        assertEquals("Alice Smith", savedUser.getName());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals("CLIENT", response.getRole());
    }

    // Rejects registration when the email already belongs to a user
    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                " Alice@Example.com ", "password123", "Alice", Role.CLIENT);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class, () -> authService.register(request));

        assertEquals("Email is already in use", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    // Logs in with a normalized email and returns a JWT for the matching user
    @Test
    void loginReturnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest(" Alice@Example.com ", "password123");
        User user = new User("user-1", "alice@example.com", "encoded-password", "Alice", Role.SELLER, null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(any(UserDetails.class), eq("user-1"))).thenReturn("test-token");

        var response = authService.login(request);

        assertEquals("test-token", response.getToken());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(Role.SELLER, response.getRole());
        verify(jwtService).generateToken(any(UserDetails.class), eq("user-1"));
    }

    // Rejects login when no account exists for the email
    @Test
    void loginRejectsUnknownEmail() {
        LoginRequest request = new LoginRequest(" Missing@Example.com ", "password123");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
            BadCredentialsException.class, () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(UserDetails.class), any());
    }

    // Rejects login when the email exists but the password does not match
    @Test
    void loginRejectsIncorrectPassword() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrong-password");
        User user = new User("user-1", "alice@example.com", "encoded-password", "Alice", Role.CLIENT, null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        BadCredentialsException exception = assertThrows(
            BadCredentialsException.class, () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtService, never()).generateToken(any(UserDetails.class), any());
    }

    // Rejects registration when required fields are null
    @Test
    void registerRequestRejectsNullRequiredFields() {
        Set<String> invalidFields = invalidFields(new RegisterRequest());

        assertTrue(invalidFields.containsAll(Set.of("email", "password", "name", "role")));
    }

    // Rejects registration when required text fields are empty
    @Test
    void registerRequestRejectsEmptyRequiredFields() {
        RegisterRequest request = new RegisterRequest("", "", "", null);

        Set<String> invalidFields = invalidFields(request);

        assertTrue(invalidFields.containsAll(Set.of("email", "password", "name", "role")));
    }

    // Rejects invalid email format a short password, and a big name
    @Test
    void registerRequestRejectsMalformedAndOutOfRangeFields() {
        RegisterRequest request = new RegisterRequest(
                "not-an-email", "short", "N".repeat(101), Role.CLIENT);

        Set<String> invalidFields = invalidFields(request);

        assertTrue(invalidFields.containsAll(Set.of("email", "password", "name")));
    }

    // Rejects registration values that is more than  the email and password limits
    @Test
    void registerRequestRejectsOversizedEmailAndPassword() {
        RegisterRequest request = new RegisterRequest(
                "a".repeat(245) + "@example.com", "p".repeat(73), "Alice", Role.CLIENT);

        Set<String> invalidFields = invalidFields(request);

        assertTrue(invalidFields.containsAll(Set.of("email", "password")));
    }

    // Rejects login requests with null required fields
    @Test
    void loginRequestRejectsNullRequiredFields() {
        Set<String> invalidFields = invalidFields(new LoginRequest());

        assertTrue(invalidFields.containsAll(Set.of("email", "password")));
    }

    // Rejects login requests with empty required fields
    @Test
    void loginRequestRejectsEmptyRequiredFields() {
        Set<String> invalidFields = invalidFields(new LoginRequest("", ""));

        assertTrue(invalidFields.containsAll(Set.of("email", "password")));
    }

    // Rejects login requests with a malformed email address
    @Test
    void loginRequestRejectsMalformedEmail() {
        Set<String> invalidFields = invalidFields(new LoginRequest("not-an-email", "password123"));

        assertTrue(invalidFields.contains("email"));
    }

    private Set<String> invalidFields(Object request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(request).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
        }
    }
}