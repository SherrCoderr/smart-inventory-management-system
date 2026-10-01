package com.inventory.service;

import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.RegisterRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.UserResponse;
import com.inventory.security.UserPrincipal;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser(UserPrincipal userPrincipal);
}
