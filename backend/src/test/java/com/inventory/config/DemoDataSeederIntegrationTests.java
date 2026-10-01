package com.inventory.config;

import com.inventory.entity.*;
import com.inventory.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DemoDataSeederIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private DemoDataSeeder seeder;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();
        userRepository.deleteAll();

        seeder = new DemoDataSeeder(
                userRepository,
                categoryRepository,
                supplierRepository,
                productRepository,
                transactionRepository,
                passwordEncoder,
                Optional.empty()
        );
    }

    @Test
    @DisplayName("A. Empty Database: Populates complete demo dataset with users, categories, suppliers, products, and transactions")
    void testSeederPopulatesCompleteDatasetWhenEnabled() {
        seeder.run();

        // 1. Verify Users
        assertEquals(3, userRepository.count());
        Optional<User> adminOpt = userRepository.findByUsername("admin_demo");
        assertTrue(adminOpt.isPresent());
        assertEquals("admin@inventory.demo", adminOpt.get().getEmail());
        assertEquals(Role.ADMIN, adminOpt.get().getRole());
        assertTrue(passwordEncoder.matches("DemoAdmin@123", adminOpt.get().getPassword()));

        Optional<User> sarahOpt = userRepository.findByUsername("sarah_inventory");
        assertTrue(sarahOpt.isPresent());
        assertEquals(Role.EMPLOYEE, sarahOpt.get().getRole());
        assertTrue(passwordEncoder.matches("DemoStaff@123", sarahOpt.get().getPassword()));

        Optional<User> marcusOpt = userRepository.findByUsername("marcus_warehouse");
        assertTrue(marcusOpt.isPresent());
        assertEquals(Role.EMPLOYEE, marcusOpt.get().getRole());

        // 2. Verify Categories & Suppliers
        assertEquals(6, categoryRepository.count());
        assertEquals(6, supplierRepository.count());

        // 3. Verify Products
        assertEquals(20, productRepository.count());

        // Verify stock non-negativity constraint
        List<Product> allProducts = productRepository.findAll();
        for (Product product : allProducts) {
            assertTrue(product.getCurrentStock() >= 0, "Stock must never be negative for SKU: " + product.getSku());
            assertTrue(product.getMinStockThreshold() >= 0, "Threshold must never be negative for SKU: " + product.getSku());
            assertNotNull(product.getCategory());
            assertNotNull(product.getSupplier());
        }

        // Verify low stock detection
        long lowStockCount = allProducts.stream()
                .filter(p -> p.getCurrentStock() <= p.getMinStockThreshold())
                .count();
        assertEquals(6, lowStockCount, "Should have exactly 6 low-stock/reorder products for demo");

        // 4. Verify Transactions
        assertEquals(30, transactionRepository.count());
    }

    @Test
    @DisplayName("B. Fully Seeded Database: Multiple consecutive runs create zero duplicates and alter nothing")
    void testSeederIsIdempotentAndDoesNotDuplicate() {
        // Run 1
        seeder.run();
        long initialUsers = userRepository.count();
        long initialCategories = categoryRepository.count();
        long initialSuppliers = supplierRepository.count();
        long initialProducts = productRepository.count();
        long initialTransactions = transactionRepository.count();

        // Run 2
        seeder.run();

        // Run 3
        seeder.run();

        // Verify counts remain perfectly identical
        assertEquals(initialUsers, userRepository.count());
        assertEquals(initialCategories, categoryRepository.count());
        assertEquals(initialSuppliers, supplierRepository.count());
        assertEquals(initialProducts, productRepository.count());
        assertEquals(initialTransactions, transactionRepository.count());
    }

    @Test
    @DisplayName("C. Partially Populated Database: Safely reuses pre-existing records and seeds only missing records")
    void testSeederHandlesPartiallyPopulatedDatabase() {
        // Pre-create 1 category and 1 supplier with matching demo names
        Category existingCat = categoryRepository.save(Category.builder()
                .name("Electronics & Hardware")
                .description("Pre-existing description")
                .build());

        Supplier existingSup = supplierRepository.save(Supplier.builder()
                .name("Apex Logistics & Tech Supply")
                .email("contact@apextech.demo")
                .phone("+1 555-000-1111")
                .address("Original Address")
                .active(true)
                .build());

        // Run seeder
        seeder.run();

        // Total categories must be 6 (1 pre-existing reused + 5 newly created)
        assertEquals(6, categoryRepository.count());
        // Description of pre-existing category must NOT be overwritten
        Category fetchedCat = categoryRepository.findByNameIgnoreCase("Electronics & Hardware").orElseThrow();
        assertEquals("Pre-existing description", fetchedCat.getDescription());
        assertEquals(existingCat.getId(), fetchedCat.getId());

        // Total suppliers must be 6 (1 pre-existing reused + 5 newly created)
        assertEquals(6, supplierRepository.count());
        Supplier fetchedSup = supplierRepository.findByEmailIgnoreCase("contact@apextech.demo").orElseThrow();
        assertEquals("+1 555-000-1111", fetchedSup.getPhone());
        assertEquals(existingSup.getId(), fetchedSup.getId());

        // Products and users should still seed normally
        assertEquals(20, productRepository.count());
        assertEquals(3, userRepository.count());
        assertEquals(30, transactionRepository.count());
    }

    @Test
    @DisplayName("D. Conflicting Real User: Existing user with demo username/email is preserved without modifying password/role")
    void testSeederPreservesConflictingRealUser() {
        // Pre-existing user that has the username "admin_demo" with custom credentials
        User realUser = User.builder()
                .username("admin_demo")
                .email("real_admin@company.org")
                .password(passwordEncoder.encode("RealCustomPassword!999"))
                .firstName("Original")
                .lastName("Operator")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(realUser);

        // Run seeder
        seeder.run();

        // The user must retain their original password and email
        User preservedUser = userRepository.findByUsername("admin_demo").orElseThrow();
        assertEquals("real_admin@company.org", preservedUser.getEmail());
        assertTrue(passwordEncoder.matches("RealCustomPassword!999", preservedUser.getPassword()));
        assertFalse(passwordEncoder.matches("DemoAdmin@123", preservedUser.getPassword()));
        assertEquals("Original", preservedUser.getFirstName());
    }

    @Test
    @DisplayName("E. Existing Production Product: Product with matching SKU has stock and price preserved")
    void testSeederDoesNotOverwriteExistingProductWithSameSku() {
        // Pre-create the category and supplier required
        Category cat = categoryRepository.save(Category.builder().name("Electronics & Hardware").build());
        Supplier sup = supplierRepository.save(Supplier.builder().name("Apex Logistics & Tech Supply").email("contact@apextech.demo").build());

        // Pre-create product DELL-LAT-5440 with custom non-demo stock and price
        Product customProduct = Product.builder()
                .sku("DELL-LAT-5440")
                .name("Custom Configured Latitude")
                .description("Production specific description")
                .price(new BigDecimal("4999.99"))
                .currentStock(999)
                .minStockThreshold(50)
                .reorderQuantity(100)
                .category(cat)
                .supplier(sup)
                .active(true)
                .build();
        productRepository.save(customProduct);

        // Run seeder
        seeder.run();

        // Verify product was not overwritten
        Product verified = productRepository.findBySkuIgnoreCase("DELL-LAT-5440").orElseThrow();
        assertEquals(999, verified.getCurrentStock());
        assertEquals(new BigDecimal("4999.99"), verified.getPrice());
        assertEquals("Custom Configured Latitude", verified.getName());
    }

    @Test
    @DisplayName("F. Redis Unavailable: Startup and seeding succeed cleanly without crashing when cache manager errors")
    void testSeederSucceedsWhenCacheFails() {
        Cache failingCache = mock(Cache.class);
        doThrow(new RuntimeException("Redis connection refused (Upstash timeout)")).when(failingCache).clear();

        CacheManager failingCacheManager = mock(CacheManager.class);
        when(failingCacheManager.getCacheNames()).thenReturn(List.of("products", "dashboard"));
        when(failingCacheManager.getCache(anyString())).thenReturn(failingCache);

        DemoDataSeeder seederWithFaultyCache = new DemoDataSeeder(
                userRepository,
                categoryRepository,
                supplierRepository,
                productRepository,
                transactionRepository,
                passwordEncoder,
                Optional.of(failingCacheManager)
        );

        // Execution must complete gracefully without propagating exception
        assertDoesNotThrow(() -> seederWithFaultyCache.run());

        // Verify data was still seeded despite cache failure
        assertEquals(20, productRepository.count());
        assertEquals(3, userRepository.count());
    }

    @Test
    @DisplayName("G. Transaction Idempotency: Running seeder multiple times does not duplicate transactions")
    void testTransactionIdempotency() {
        seeder.run();
        assertEquals(30, transactionRepository.count());

        // Run second and third time
        seeder.run();
        seeder.run();

        assertEquals(30, transactionRepository.count());
    }

    @Test
    @DisplayName("H. Non-negative stock invariant remains valid across all seeded products")
    void testStockNonNegativityInvariant() {
        seeder.run();

        List<Product> products = productRepository.findAll();
        assertFalse(products.isEmpty());
        for (Product product : products) {
            assertTrue(product.getCurrentStock() >= 0, "Product stock must be non-negative: " + product.getSku());
            assertTrue(product.getMinStockThreshold() >= 0, "Product min threshold must be non-negative: " + product.getSku());
            assertTrue(product.getReorderQuantity() >= 0, "Product reorder quantity must be non-negative: " + product.getSku());
        }
    }
}
