package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.CategoryRequest;
import com.inventory.entity.Category;
import com.inventory.entity.Role;
import com.inventory.entity.User;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        for (String name : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }

        transactionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .username("admin_cat")
                .email("admin_cat@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .active(true)
                .build());
        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(admin.getId(), admin.getUsername(), admin.getRole().name());

        User employee = userRepository.save(User.builder()
                .username("employee_cat")
                .email("employee_cat@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.EMPLOYEE)
                .active(true)
                .build());
        employeeToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(employee.getId(), employee.getUsername(), employee.getRole().name());
    }

    @Test
    @DisplayName("Admin can create category successfully")
    void testAdminCreateCategory() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .description("Smart devices")
                .build();

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Electronics")));
    }

    @Test
    @DisplayName("Employee is forbidden from creating category (403)")
    void testEmployeeForbiddenCreateCategory() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Furniture")
                .build();

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Reject duplicate category name (409)")
    void testDuplicateCategoryRejection() throws Exception {
        categoryRepository.save(Category.builder().name("Books").build());

        CategoryRequest request = CategoryRequest.builder()
                .name("Books")
                .build();

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Employee can read categories")
    void testEmployeeCanReadCategories() throws Exception {
        categoryRepository.save(Category.builder().name("Apparel").build());

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Apparel")));
    }
}
