package com.inventory.config;

import com.inventory.entity.Product;
import com.inventory.entity.Role;
import com.inventory.entity.User;
import com.inventory.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("Should populate complete demo dataset with users, categories, suppliers, products, and transactions")
    void testSeederPopulatesCompleteDatasetWhenEnabled() {
        // Execute seeder
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
    @DisplayName("Should be idempotent and not duplicate data when executed multiple times")
    void testSeederIsIdempotentAndDoesNotDuplicate() {
        // Run first time
        seeder.run();
        long initialUsers = userRepository.count();
        long initialCategories = categoryRepository.count();
        long initialSuppliers = supplierRepository.count();
        long initialProducts = productRepository.count();
        long initialTransactions = transactionRepository.count();

        // Run second time
        seeder.run();

        // Verify counts remain unchanged
        assertEquals(initialUsers, userRepository.count());
        assertEquals(initialCategories, categoryRepository.count());
        assertEquals(initialSuppliers, supplierRepository.count());
        assertEquals(initialProducts, productRepository.count());
        assertEquals(initialTransactions, transactionRepository.count());
    }

    @Test
    @DisplayName("Should not delete or modify pre-existing production user data")
    void testSeederPreservesPreExistingUserData() {
        // Create an existing custom user prior to seeding
        User existingUser = User.builder()
                .username("custom_production_user")
                .email("prod_user@company.com")
                .password(passwordEncoder.encode("SecurePass123"))
                .firstName("Real")
                .lastName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(existingUser);

        // Run seeder
        seeder.run();

        // Verify pre-existing user still exists with original credentials
        Optional<User> found = userRepository.findByUsername("custom_production_user");
        assertTrue(found.isPresent());
        assertEquals("prod_user@company.com", found.get().getEmail());
        assertTrue(passwordEncoder.matches("SecurePass123", found.get().getPassword()));

        // Verify total users = 1 pre-existing + 3 demo
        assertEquals(4, userRepository.count());
    }
}
