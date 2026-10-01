package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.ProductRequest;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Role;
import com.inventory.entity.Supplier;
import com.inventory.entity.User;
import com.inventory.repository.CategoryRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTests {

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
                .username("admin_prod")
                .email("admin_prod@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .active(true)
                .build());
        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(admin.getId(), admin.getUsername(), admin.getRole().name());

        User employee = userRepository.save(User.builder()
                .username("employee_prod")
                .email("employee_prod@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.EMPLOYEE)
                .active(true)
                .build());
        employeeToken = "Bearer " + jwtTokenProvider.generateTokenFromUser(employee.getId(), employee.getUsername(), employee.getRole().name());

        savedCategory = categoryRepository.save(Category.builder().name("Hardware").build());
        savedSupplier = supplierRepository.save(Supplier.builder().name("Prime Supplies").email("prime@supplies.com").build());
    }

    @Test
    @DisplayName("Admin can create product successfully")
    void testAdminCreateProduct() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-100")
                .name("Mechanical Keyboard")
                .description("RGB Gaming Keyboard")
                .price(new BigDecimal("79.99"))
                .currentStock(25)
                .minStockThreshold(5)
                .reorderQuantity(15)
                .categoryId(savedCategory.getId())
                .supplierId(savedSupplier.getId())
                .build();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sku", is("SKU-100")))
                .andExpect(jsonPath("$.data.name", is("Mechanical Keyboard")))
                .andExpect(jsonPath("$.data.currentStock", is(25)))
                .andExpect(jsonPath("$.data.lowStock", is(false)))
                .andExpect(jsonPath("$.data.category.name", is("Hardware")))
                .andExpect(jsonPath("$.data.supplier.name", is("Prime Supplies")));
    }

    @Test
    @DisplayName("Reject duplicate SKU (409)")
    void testDuplicateSkuRejection() throws Exception {
        productRepository.save(Product.builder()
                .sku("SKU-EXISTING")
                .name("Existing Product")
                .price(BigDecimal.TEN)
                .category(savedCategory)
                .supplier(savedSupplier)
                .build());

        ProductRequest request = ProductRequest.builder()
                .sku("SKU-EXISTING")
                .name("Another Product")
                .price(BigDecimal.ONE)
                .categoryId(savedCategory.getId())
                .supplierId(savedSupplier.getId())
                .build();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Product already exists with SKU: 'SKU-EXISTING'")));
    }

    @Test
    @DisplayName("Reject invalid product payload with negative price/stock (400)")
    void testInvalidProductPayload() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("")
                .name("")
                .price(new BigDecimal("-10.00"))
                .currentStock(-5)
                .build();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.data.sku", notNullValue()))
                .andExpect(jsonPath("$.data.price", notNullValue()))
                .andExpect(jsonPath("$.data.currentStock", notNullValue()));
    }

    @Test
    @DisplayName("Search products with pagination and category filter")
    void testSearchProducts() throws Exception {
        productRepository.save(Product.builder()
                .sku("LAPTOP-01")
                .name("Gaming Laptop RTX")
                .price(new BigDecimal("1200.00"))
                .currentStock(8)
                .minStockThreshold(10) // low stock
                .category(savedCategory)
                .supplier(savedSupplier)
                .active(true)
                .build());

        mockMvc.perform(get("/api/products?query=Gaming&categoryId=" + savedCategory.getId())
                        .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku", is("LAPTOP-01")))
                .andExpect(jsonPath("$.data.content[0].lowStock", is(true)));
    }

    @Test
    @DisplayName("Lookup product by SKU")
    void testGetProductBySku() throws Exception {
        productRepository.save(Product.builder()
                .sku("MOUSE-99")
                .name("Ergonomic Mouse")
                .price(new BigDecimal("29.99"))
                .category(savedCategory)
                .supplier(savedSupplier)
                .build());

        mockMvc.perform(get("/api/products/sku/MOUSE-99")
                        .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Ergonomic Mouse")));
    }

    @Test
    @DisplayName("Admin can soft-delete product")
    void testDeleteProduct() throws Exception {
        Product p = productRepository.save(Product.builder()
                .sku("TO-DELETE")
                .name("Old Product")
                .price(BigDecimal.ONE)
                .category(savedCategory)
                .supplier(savedSupplier)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/products/" + p.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(p.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(updated.isActive());
    }
}
