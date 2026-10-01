package com.inventory.controller;

import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.RegisterRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.UserResponse;
import com.inventory.security.UserPrincipal;
import com.inventory.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponse userResponse = authService.getCurrentUser(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved successfully", userResponse));
    }
}
