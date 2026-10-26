package com.userservice.userservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.userservice.userservice.dto.UserResponse;
import com.userservice.userservice.dto.UserUpdateRequest;
import com.userservice.userservice.exception.GlobalExceptionHandler;
import com.userservice.userservice.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String EMAIL = "alice@example.com";

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void getProfileReturnsTheAuthenticatedUsersProfile() throws Exception {
        when(userService.getProfile(EMAIL))
                .thenReturn(new UserResponse("user-1", EMAIL, "Alice", "CLIENT", "avatar.png"));

        mockMvc.perform(get("/me").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.avatar").value("avatar.png"));

        verify(userService).getProfile(EMAIL);
    }

    @Test
    void updateProfilePassesRequestForAuthenticatedUser() throws Exception {
        when(userService.updateProfile(eq(EMAIL), any(UserUpdateRequest.class)))
                .thenReturn(new UserResponse("user-1", EMAIL, "Alice Updated", "CLIENT", "avatar.png"));

        mockMvc.perform(put("/me")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice Updated",
                                  "avatar": "avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.avatar").value("avatar.png"));

        verify(userService).updateProfile(eq(EMAIL), any(UserUpdateRequest.class));
    }

    @Test
    void updateProfileRejectsBlankNameWithoutCallingService() throws Exception {
        mockMvc.perform(put("/me")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "avatar": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateProfile(eq(EMAIL), any(UserUpdateRequest.class));
    }

    @Test
    void updateAvatarPassesAvatarForAuthenticatedUser() throws Exception {
        when(userService.updateAvatar(EMAIL, "https://example.com/avatar.png"))
                .thenReturn(new UserResponse(
                        "user-1", EMAIL, "Alice", "CLIENT", "https://example.com/avatar.png"));

        mockMvc.perform(put("/me/avatar")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatar": "https://example.com/avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").value("https://example.com/avatar.png"));

        verify(userService).updateAvatar(EMAIL, "https://example.com/avatar.png");
    }

    @Test
    void updateAvatarRejectsBlankAvatarWithoutCallingService() throws Exception {
        mockMvc.perform(put("/me/avatar")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatar": " "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateAvatar(eq(EMAIL), any(String.class));
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(EMAIL, "test-password");
    }
}
