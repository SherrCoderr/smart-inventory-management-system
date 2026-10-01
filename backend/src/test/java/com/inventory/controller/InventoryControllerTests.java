package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.StockOperationRequest;
import com.inventory.entity.*;
import com.inventory.repository.*;
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

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerTests {

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
    private Product testProduct;
    private Product lowStockProduct;
    private Category savedCategory;
    private Supplier savedSupplier;

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

        User admin = userRepository.save(User.builder()
                .username("admin_inv")
                .email("admin_inv@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .active(true)
                .build());
        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(admin.getId(), admin.getUsername(), admin.getRole().name());

        User employee = userRepository.save(User.builder()
                .username("employee_inv")
                .email("employee_inv@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.EMPLOYEE)
                .active(true)
                .build());
        employeeToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(employee.getId(), employee.getUsername(), employee.getRole().name());

        savedCategory = categoryRepository.save(Category.builder().name("Components").build());
        savedSupplier = supplierRepository.save(Supplier.builder().name("Apex Logix").email("apex@logix.com").build());

        testProduct = productRepository.save(Product.builder()
                .sku("CPU-AMD-5800X")
                .name("AMD Ryzen 7 5800X")
                .price(new BigDecimal("299.99"))
                .currentStock(20)
                .minStockThreshold(5)
                .reorderQuantity(10)
                .category(savedCategory)
                .supplier(savedSupplier)
                .active(true)
                .build());

        lowStockProduct = productRepository.save(Product.builder()
                .sku("GPU-NV-4070")
                .name("NVIDIA RTX 4070")
                .price(new BigDecimal("599.99"))
                .currentStock(3) // <= threshold 5
                .minStockThreshold(5)
                .reorderQuantity(15)
                .category(savedCategory)
                .supplier(savedSupplier)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("STOCK_IN - Successfully increase stock and record audit transaction")
    void testStockInSuccess() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(10)
                .referenceReason("Restock Shipment PO-909")
                .build();

        mockMvc.perform(post("/api/inventory/stock-in")
                        .header("Authorization", employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.transactionType", is("STOCK_IN")))
                .andExpect(jsonPath("$.data.quantity", is(10)))
                .andExpect(jsonPath("$.data.previousStock", is(20)))
                .andExpect(jsonPath("$.data.newStock", is(30)))
                .andExpect(jsonPath("$.data.username", is("employee_inv")));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualTo(30);

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("STOCK_IN - Reject invalid zero or negative quantity")
    void testStockInInvalidQuantity() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(-5)
                .build();

        mockMvc.perform(post("/api/inventory/stock-in")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.data.quantity", notNullValue()));
    }

    @Test
    @DisplayName("STOCK_IN - Reject stock-in on inactive product")
    void testStockInInactiveProduct() throws Exception {
        testProduct.setActive(false);
        productRepository.save(testProduct);

        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(5)
                .build();

        mockMvc.perform(post("/api/inventory/stock-in")
                        .header("Authorization", employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("inactive product")));
    }

    @Test
    @DisplayName("STOCK_OUT - Successfully decrease stock and record audit transaction")
    void testStockOutSuccess() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(8)
                .referenceReason("Sales Order SO-1002")
                .build();

        mockMvc.perform(post("/api/inventory/stock-out")
                        .header("Authorization", employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.transactionType", is("STOCK_OUT")))
                .andExpect(jsonPath("$.data.quantity", is(8)))
                .andExpect(jsonPath("$.data.previousStock", is(20)))
                .andExpect(jsonPath("$.data.newStock", is(12)));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualTo(12);

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("STOCK_OUT - Reject operation when requested quantity exceeds available stock")
    void testStockOutInsufficientStock() throws Exception {
        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(25) // Available is 20
                .build();

        mockMvc.perform(post("/api/inventory/stock-out")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Insufficient stock for product SKU 'CPU-AMD-5800X'")));

        Product unchanged = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(unchanged.getCurrentStock()).isEqualTo(20);
        assertThat(transactionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/inventory/transactions - Retrieve paginated audit trail")
    void testGetTransactions() throws Exception {
        // Create 2 transactions
        StockOperationRequest inReq = StockOperationRequest.builder().productId(testProduct.getId()).quantity(5).build();
        StockOperationRequest outReq = StockOperationRequest.builder().productId(testProduct.getId()).quantity(2).build();

        mockMvc.perform(post("/api/inventory/stock-in")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inReq))).andExpect(status().isOk());

        mockMvc.perform(post("/api/inventory/stock-out")
                .header("Authorization", employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outReq))).andExpect(status().isOk());

        mockMvc.perform(get("/api/inventory/transactions?productId=" + testProduct.getId())
                        .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(2)));
    }

    @Test
    @DisplayName("GET /api/inventory/low-stock - Return products below or equal to threshold")
    void testGetLowStockProducts() throws Exception {
        mockMvc.perform(get("/api/inventory/low-stock")
                        .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku", is("GPU-NV-4070")))
                .andExpect(jsonPath("$.data.content[0].currentStock", is(3)))
                .andExpect(jsonPath("$.data.content[0].lowStock", is(true)));
    }

    @Test
    @DisplayName("GET /api/inventory/reorder-recommendations - Generate deterministic replenishment recommendations")
    void testGetReorderRecommendations() throws Exception {
        // lowStockProduct: stock = 3, threshold = 5, baseReorder = 15
        // Deficit = 5 - 3 = 2
        // Suggested = max(15, 2 + 15) = 17
        mockMvc.perform(get("/api/inventory/reorder-recommendations")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sku", is("GPU-NV-4070")))
                .andExpect(jsonPath("$.data[0].deficit", is(2)))
                .andExpect(jsonPath("$.data[0].suggestedReorderQuantity", is(17)))
                .andExpect(jsonPath("$.data[0].supplier.name", is("Apex Logix")));
    }

    @Test
    @DisplayName("CONCURRENCY - Concurrent stock operations never produce negative stock")
    void testConcurrentStockOutOperations() throws Exception {
        int threads = 10;
        int initialStock = 15;
        testProduct.setCurrentStock(initialStock);
        productRepository.save(testProduct);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successfulOuts = new AtomicInteger(0);
        AtomicInteger failedOuts = new AtomicInteger(0);

        StockOperationRequest request = StockOperationRequest.builder()
                .productId(testProduct.getId())
                .quantity(2) // 10 requests * 2 = 20 requested, only 7 can succeed (7 * 2 = 14 <= 15)
                .referenceReason("Concurrent Checkout")
                .build();

        String payload = objectMapper.writeValueAsString(request);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/api/inventory/stock-out")
                                    .header("Authorization", employeeToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successfulOuts.incrementAndGet();
                                } else {
                                    failedOuts.incrementAndGet();
                                }
                            });
                } catch (Exception ignored) {
                    failedOuts.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(finalProduct.getCurrentStock()).isGreaterThanOrEqualTo(0);
        assertThat(finalProduct.getCurrentStock()).isEqualTo(initialStock - (successfulOuts.get() * 2));
        assertThat(successfulOuts.get()).isEqualTo(7);
        assertThat(failedOuts.get()).isEqualTo(3);
        assertThat(transactionRepository.count()).isEqualTo(7);
    }
}
