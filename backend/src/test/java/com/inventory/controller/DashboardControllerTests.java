package com.inventory.controller;

import com.inventory.entity.*;
import com.inventory.repository.*;
import com.inventory.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
    private User adminUser;

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
        supplierRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .username("admin_dash")
                .email("admin_dash@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .active(true)
                .build());
        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(adminUser.getId(), adminUser.getUsername(), adminUser.getRole().name());

        User employeeUser = userRepository.save(User.builder()
                .username("employee_dash")
                .email("employee_dash@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.EMPLOYEE)
                .active(true)
                .build());
        employeeToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(employeeUser.getId(), employeeUser.getUsername(), employeeUser.getRole().name());
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - Return clean zeros on empty database")
    void testEmptyDashboardStats() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProducts", is(0)))
                .andExpect(jsonPath("$.data.totalSuppliers", is(0)))
                .andExpect(jsonPath("$.data.totalCategories", is(0)))
                .andExpect(jsonPath("$.data.totalInventoryValue", is(0)))
                .andExpect(jsonPath("$.data.lowStockProductCount", is(0)))
                .andExpect(jsonPath("$.data.totalStockInQuantity", is(0)))
                .andExpect(jsonPath("$.data.totalStockOutQuantity", is(0)))
                .andExpect(jsonPath("$.data.dailyMovements", hasSize(7)));
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - Accurate calculation of inventory value, counts, and movement metrics")
    void testPopulatedDashboardStats() throws Exception {
        Category cat = categoryRepository.save(Category.builder().name("Monitors").build());
        Supplier sup = supplierRepository.save(Supplier.builder().name("ViewSonic").email("view@sonic.com").active(true).build());

        // Product 1: 10 units @ $200 = $2,000 (normal stock, threshold 5)
        Product p1 = productRepository.save(Product.builder()
                .sku("MON-4K-01")
                .name("4K IPS Monitor")
                .price(new BigDecimal("200.00"))
                .currentStock(10)
                .minStockThreshold(5)
                .reorderQuantity(10)
                .category(cat)
                .supplier(sup)
                .active(true)
                .build());

        // Product 2: 2 units @ $150 = $300 (low stock: 2 <= 5)
        Product p2 = productRepository.save(Product.builder()
                .sku("MON-1080P-02")
                .name("1080P Gaming Monitor")
                .price(new BigDecimal("150.00"))
                .currentStock(2)
                .minStockThreshold(5)
                .reorderQuantity(8)
                .category(cat)
                .supplier(sup)
                .active(true)
                .build());

        // Record stock movements: 15 in, 5 out
        transactionRepository.save(InventoryTransaction.builder()
                .product(p1)
                .user(adminUser)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(15)
                .previousStock(0)
                .newStock(15)
                .build());

        transactionRepository.save(InventoryTransaction.builder()
                .product(p1)
                .user(adminUser)
                .transactionType(TransactionType.STOCK_OUT)
                .quantity(5)
                .previousStock(15)
                .newStock(10)
                .build());

        // Total Inventory Value = (10 * 200) + (2 * 150) = 2000 + 300 = 2300.00
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProducts", is(2)))
                .andExpect(jsonPath("$.data.activeProducts", is(2)))
                .andExpect(jsonPath("$.data.totalSuppliers", is(1)))
                .andExpect(jsonPath("$.data.totalCategories", is(1)))
                .andExpect(jsonPath("$.data.totalInventoryValue", is(2300.00)))
                .andExpect(jsonPath("$.data.lowStockProductCount", is(1)))
                .andExpect(jsonPath("$.data.totalStockInQuantity", is(15)))
                .andExpect(jsonPath("$.data.totalStockOutQuantity", is(5)))
                .andExpect(jsonPath("$.data.recentTransactions", hasSize(2)))
                .andExpect(jsonPath("$.data.dailyMovements", hasSize(7)));
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - Reject unauthenticated access (401)")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}
