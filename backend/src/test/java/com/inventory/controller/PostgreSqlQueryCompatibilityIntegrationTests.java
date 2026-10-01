package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.entity.*;
import com.inventory.repository.*;
import com.inventory.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PostgreSqlQueryCompatibilityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CacheManager cacheManager;

    private String adminToken;
    private User savedAdmin;
    private Category savedCategory;
    private Supplier savedSupplier;
    private Product savedProduct1;
    private Product savedProduct2;

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

        savedAdmin = userRepository.save(User.builder()
                .username("compat_admin")
                .email("compat_admin@inventory.test")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.ADMIN)
                .active(true)
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(
                savedAdmin.getId(), savedAdmin.getUsername(), savedAdmin.getRole().name()
        );

        savedCategory = categoryRepository.save(Category.builder()
                .name("Enterprise Hardware")
                .description("Servers and peripherals")
                .build());

        savedSupplier = supplierRepository.save(Supplier.builder()
                .name("Global Tech Supply")
                .email("orders@globaltech.test")
                .phone("+1 555-0199")
                .address("100 Innovation Way")
                .active(true)
                .build());

        savedProduct1 = productRepository.save(Product.builder()
                .sku("PROD-SRV-001")
                .name("Enterprise Rack Server 1U")
                .description("Dual Xeon 64GB")
                .price(new BigDecimal("2500.00"))
                .currentStock(10)
                .minStockThreshold(5)
                .reorderQuantity(5)
                .category(savedCategory)
                .supplier(savedSupplier)
                .active(true)
                .build());

        savedProduct2 = productRepository.save(Product.builder()
                .sku("PROD-NET-002")
                .name("Gigabit Managed Switch 48-Port")
                .description("L3 Managed Switch")
                .price(new BigDecimal("800.00"))
                .currentStock(2) // low stock
                .minStockThreshold(6)
                .reorderQuantity(10)
                .category(savedCategory)
                .supplier(savedSupplier)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("GET /api/products without query returns all products (no null parameter errors)")
    void testGetProductsNoQuery() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(2)));
    }

    @Test
    @DisplayName("GET /api/products with query filters by name or SKU properly")
    void testGetProductsWithQuery() throws Exception {
        mockMvc.perform(get("/api/products?query=Rack")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku", is("PROD-SRV-001")));

        mockMvc.perform(get("/api/products?query=PROD-NET")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku", is("PROD-NET-002")));
    }

    @Test
    @DisplayName("GET /api/suppliers without search returns all suppliers")
    void testGetSuppliersNoSearch() throws Exception {
        mockMvc.perform(get("/api/suppliers")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Global Tech Supply")));
    }

    @Test
    @DisplayName("GET /api/suppliers with search filters correctly")
    void testGetSuppliersWithSearch() throws Exception {
        mockMvc.perform(get("/api/suppliers?search=Global")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(get("/api/suppliers?search=NonExistent")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/inventory/transactions without filters returns transactions")
    void testGetTransactionsNoFilters() throws Exception {
        transactionRepository.save(InventoryTransaction.builder()
                .product(savedProduct1)
                .user(savedAdmin)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .previousStock(0)
                .newStock(10)
                .referenceReason("Initial shipment")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        mockMvc.perform(get("/api/inventory/transactions")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].productSku", is("PROD-SRV-001")))
                .andExpect(jsonPath("$.data.content[0].transactionType", is("STOCK_IN")));
    }

    @Test
    @DisplayName("GET /api/inventory/transactions with individual and combined filters")
    void testGetTransactionsWithFilters() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        transactionRepository.save(InventoryTransaction.builder()
                .product(savedProduct1)
                .user(savedAdmin)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .previousStock(0)
                .newStock(10)
                .referenceReason("Restock")
                .createdAt(now.minusHours(5))
                .build());

        transactionRepository.save(InventoryTransaction.builder()
                .product(savedProduct2)
                .user(savedAdmin)
                .transactionType(TransactionType.STOCK_OUT)
                .quantity(2)
                .previousStock(4)
                .newStock(2)
                .referenceReason("Dispatch")
                .createdAt(now.minusHours(2))
                .build());

        // Filter by productId
        mockMvc.perform(get("/api/inventory/transactions?productId=" + savedProduct1.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].productSku", is("PROD-SRV-001")));

        // Filter by transactionType
        mockMvc.perform(get("/api/inventory/transactions?transactionType=STOCK_OUT")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].productSku", is("PROD-NET-002")));

        // Filter by date range
        String startStr = now.minusDays(1).toString();
        String endStr = now.plusDays(1).toString();
        mockMvc.perform(get("/api/inventory/transactions?startDate=" + startStr + "&endDate=" + endStr)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/dashboard/stats succeeds when there are transactions")
    void testDashboardStatsWithTransactions() throws Exception {
        transactionRepository.save(InventoryTransaction.builder()
                .product(savedProduct1)
                .user(savedAdmin)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(10)
                .previousStock(0)
                .newStock(10)
                .referenceReason("Restock")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build());

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProducts", is(2)))
                .andExpect(jsonPath("$.data.totalSuppliers", is(1)))
                .andExpect(jsonPath("$.data.totalCategories", is(1)))
                .andExpect(jsonPath("$.data.totalInventoryValue", is(26600.00))) // 10*2500 + 2*800 = 25000 + 1600 = 26600.00
                .andExpect(jsonPath("$.data.lowStockProductCount", is(1)))
                .andExpect(jsonPath("$.data.totalStockInQuantity", is(10)))
                .andExpect(jsonPath("$.data.recentTransactions", hasSize(1)))
                .andExpect(jsonPath("$.data.dailyMovements", hasSize(7)));
    }

    @Test
    @DisplayName("GET /api/dashboard/stats succeeds when there are NO transactions")
    void testDashboardStatsNoTransactions() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProducts", is(2)))
                .andExpect(jsonPath("$.data.totalStockInQuantity", is(0)))
                .andExpect(jsonPath("$.data.totalStockOutQuantity", is(0)))
                .andExpect(jsonPath("$.data.recentTransactions", hasSize(0)))
                .andExpect(jsonPath("$.data.dailyMovements", hasSize(7)));
    }

    @Test
    @DisplayName("GET /api/inventory/low-stock returns only low stock items")
    void testLowStockEndpoint() throws Exception {
        mockMvc.perform(get("/api/inventory/low-stock")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku", is("PROD-NET-002")));
    }

    @Test
    @DisplayName("GET /api/inventory/reorder-recommendations returns suggestions for low stock items")
    void testReorderRecommendationsEndpoint() throws Exception {
        mockMvc.perform(get("/api/inventory/reorder-recommendations")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sku", is("PROD-NET-002")))
                .andExpect(jsonPath("$.data[0].currentStock", is(2)))
                .andExpect(jsonPath("$.data[0].minStockThreshold", is(6)));
    }
}
