package com.inventory.service.impl;

import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.RegisterRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.UserResponse;
import com.inventory.entity.Role;
import com.inventory.entity.User;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtTokenProvider;
import com.inventory.security.UserPrincipal;
import com.inventory.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration request for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role userRole = request.getRole() != null ? request.getRole() : Role.EMPLOYEE;

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(userRole)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        String token = jwtTokenProvider.generateTokenFromUser(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login request for user: {}", request.getUsernameOrEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail().trim(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new ResourceNotFoundException("No authenticated user found in context");
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        return UserResponse.fromEntity(user);
    }
}
