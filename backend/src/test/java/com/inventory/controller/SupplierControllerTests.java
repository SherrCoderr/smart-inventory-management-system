package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.SupplierRequest;
import com.inventory.entity.Role;
import com.inventory.entity.Supplier;
import com.inventory.entity.User;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
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
class SupplierControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

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
        supplierRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .username("admin_sup")
                .email("admin_sup@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .active(true)
                .build());
        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(admin.getId(), admin.getUsername(), admin.getRole().name());

        User employee = userRepository.save(User.builder()
                .username("employee_sup")
                .email("employee_sup@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.EMPLOYEE)
                .active(true)
                .build());
        employeeToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(employee.getId(), employee.getUsername(), employee.getRole().name());
    }

    @Test
    @DisplayName("Admin can create supplier successfully")
    void testCreateSupplierSuccess() throws Exception {
        SupplierRequest request = SupplierRequest.builder()
                .name("Acme Supplies")
                .email("contact@acme.com")
                .phone("123-456-7890")
                .address("123 Acme Way")
                .build();

        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Acme Supplies")))
                .andExpect(jsonPath("$.data.email", is("contact@acme.com")));
    }

    @Test
    @DisplayName("Reject duplicate supplier email (409)")
    void testDuplicateSupplierEmail() throws Exception {
        supplierRepository.save(Supplier.builder()
                .name("Existing Supplier")
                .email("dup@supplier.com")
                .build());

        SupplierRequest request = SupplierRequest.builder()
                .name("New Supplier")
                .email("dup@supplier.com")
                .build();

        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Employee can list suppliers with pagination")
    void testEmployeeListSuppliers() throws Exception {
        supplierRepository.save(Supplier.builder().name("Supplier A").email("a@sup.com").active(true).build());
        supplierRepository.save(Supplier.builder().name("Supplier B").email("b@sup.com").active(true).build());

        mockMvc.perform(get("/api/suppliers?page=0&size=10")
                        .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(2)));
    }
}
