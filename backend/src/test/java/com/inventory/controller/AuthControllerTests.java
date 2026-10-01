package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.RegisterRequest;
import com.inventory.entity.Role;
import com.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register - Successfully register new user")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("User registered successfully")))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.username", is("john_doe")))
                .andExpect(jsonPath("$.data.user.email", is("john@example.com")))
                .andExpect(jsonPath("$.data.user.role", is("ADMIN")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Fail with validation errors on invalid input")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("jo") // < 3 chars
                .email("invalid-email") // invalid email
                .password("123") // < 6 chars
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.data.username", notNullValue()))
                .andExpect(jsonPath("$.data.email", notNullValue()))
                .andExpect(jsonPath("$.data.password", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/register - Reject duplicate username")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .username("jane_doe")
                .email("another_jane@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("User already exists with username: 'jane_doe'")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Successfully login and return JWT")
    void testLoginSuccess() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("alice_smith")
                .email("alice@example.com")
                .password("securePass123")
                .role(Role.EMPLOYEE)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("alice_smith")
                .password("securePass123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.role", is("EMPLOYEE")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Fail with wrong credentials")
    void testLoginFailureWrongPassword() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("bob_builder")
                .email("bob@example.com")
                .password("correctPassword")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("bob_builder")
                .password("wrongPassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid username/email or password")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Access profile with valid JWT Bearer token")
    void testGetCurrentUserWithToken() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("charlie_brown")
                .email("charlie@example.com")
                .password("password123")
                .firstName("Charlie")
                .lastName("Brown")
                .role(Role.EMPLOYEE)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("data").get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("charlie_brown")))
                .andExpect(jsonPath("$.data.firstName", is("Charlie")))
                .andExpect(jsonPath("$.data.lastName", is("Brown")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Reject request without Authorization header")
    void testGetCurrentUserWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
